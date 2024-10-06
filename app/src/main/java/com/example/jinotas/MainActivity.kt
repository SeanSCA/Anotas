package com.example.jinotas

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.ActivityMainBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.websocket.WebSocketClient
import com.example.jinotas.websocket.WebSocketListener
import com.example.jinotas.websocket.WebSocketService
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope, WebSocketListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var adapterNotes: AdapterNotes
    private var notesCounter: String? = null
    private var job: Job = Job()
    private lateinit var fragment: NotesFragment
    private var canConnect: Boolean = false
    val dotenv = dotenv {
        directory = "/assets"
        filename = "env" // instead of '.env', use 'env'
    }
    private val webSocketClient = WebSocketClient(
        dotenv["WEB_SOCKET_CLIENT"], lifecycleScope
    )

    //Notifications
    private val channelId = "i.apps.notifications"
    private val notificationPermissionCode = 250

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        lifecycleScope.launch {
            webSocketClient.disconnect()
        }
    }

    override fun onConnected() {
        Log.e("WebSocket", "Connected")
        // Ejecutar en el hilo principal
        var toDownload = false
        var counter = 0
        runOnUiThread {
            Toast.makeText(this, "Conectado", Toast.LENGTH_SHORT).show()
            if (tryConnection()) {
                runBlocking {
                    val corrutina = launch {
                        db = AppDatabase.getDatabase(this@MainActivity)
                        val notesListDB = db.noteDAO().getNotesList() as ArrayList<Note>
                        val notesListApi = CrudApi().getNotesList() as ArrayList<Note>
                        if (notesListApi.size > 0) {
                            for (n in notesListApi) {
                                if (notesListDB.none { it.code == n.code }) {
                                    counter++
                                    toDownload = true
                                }
                            }
                        }
                        if (toDownload) {
                            Toast.makeText(
                                this@MainActivity,
                                "Tienes $counter nuevas que descargar",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    corrutina.join()
                }
            }
        }
    }

    override fun onMessage(message: String) {
        Log.e("WebSocket", "Message received: $message")
    }

    override fun onDisconnected() {
        Log.e("WebSocket", "Disconnected")
        // Ejecutar en el hilo principal
        runOnUiThread {
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
        }
//        lifecycleScope.launch(Dispatchers.Main) {
//            webSocketClient.connect(this@MainActivity)
//        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        // Solicitar permisos de notificación si no están concedidos
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationPermissionCode
            )
        } else {
            Toast.makeText(this, "Necesitas aceptar los permisos", Toast.LENGTH_LONG).show()
        }
        //Esto es para las notificaciones
        val serviceIntent = Intent(this, WebSocketService::class.java)
        startForegroundService(serviceIntent)

        lifecycleScope.launch(Dispatchers.Main) {
            webSocketClient.connect(this@MainActivity)
        }



        binding.btDownloadNotesApi.setOnClickListener {
            downloadNotesApi()
        }

        updateNotesCounter()
        binding.btCreateNote.setOnClickListener {
            val intent = Intent(this, WriteNotesActivity::class.java)
            startActivity(intent)
        }

        binding.btSearchNote.setOnClickListener {
            showPopupMenuSearch(this@MainActivity, binding.btSearchNote)
        }

        binding.btOrderBy.setOnClickListener {
            showPopupMenuOrderBy(binding.btOrderBy)
        }

        binding.btUploadNotesApi.setOnClickListener {
            uploadNotesApi()
        }

        binding.btDeleteApi.setOnClickListener {
            deleteAllNotesApi()
        }
    }

    /**
     * Here it reloads all the notes when the app returns to this activity
     */
    override fun onResume() {
        super.onResume()
        fragment =
            (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!
        fragment.loadNotes()
    }

    /**
     * Here updates the notes counter
     */
    private fun notesCounter() {
        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(this@MainActivity)
                notesCounter = db.noteDAO().getNotesCount().toString() + " notas"
            }
            corrutina.join()
        }
        binding.notesCounter.text = notesCounter
    }

    /**
     * Here updates the notes counter every 0.5 seconds
     */
    private fun updateNotesCounter() {
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                notesCounter()
                mainHandler.postDelayed(this, 500)
            }
        })
    }

    /**
     * shows a popup with an edit text where you can write the title of the note to search
     * @param context The activity context
     * @param view The view to anchor the popup
     */
    private fun showPopupMenuSearch(context: Context, view: View) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.menu_search, null)

        // initialize the EditText field
        val searchNote = layout.findViewById<EditText>(R.id.etSearchNote)

        // create a PopupWindow
        val popup = PopupWindow(
            layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )

        // set the background color of the PopupWindow
        popup.setBackgroundDrawable(ContextCompat.getDrawable(context, R.color.white))

        // set a touch listener on the popup window so it will be dismissed when touched outside
        popup.isOutsideTouchable = true
        popup.isTouchable = true


        fragment =
            (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!
        fragment.apply {
            searchNote.afterTextChanged {
                loadFilteredNotes(searchNote.text.toString())
            }
        }
        // display the popup window at the specified location
        popup.showAsDropDown(view)
    }


    /**
     * shows a popup with a few options to order the notes
     * @param view The view to anchor the popup
     */
    fun showPopupMenuOrderBy(view: View) {
        fragment =
            (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!
        val popupMenu = PopupMenu(this@MainActivity, view)
        popupMenu.menuInflater.inflate(R.menu.popup_menu_order_by, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_order_by_date -> runBlocking {
                    val corrutina = launch {
                        db = AppDatabase.getDatabase(this@MainActivity)
                        fragment.orderByNotes("date")
                    }
                    corrutina.join()
                }

                R.id.action_order_by_title -> runBlocking {
                    val corrutina = launch {
                        db = AppDatabase.getDatabase(this@MainActivity)
                        fragment.orderByNotes("title")
                    }
                    corrutina.join()
                }
            }
            true
        }
        popupMenu.show()
        true
    }

    /**
     * Here checks if there's connection to the api
     * @return Boolean if there's connection or not
     */
    fun tryConnection(): Boolean {
        try {
            canConnect = CrudApi().canConnectToApi()
        } catch (e: Exception) {
            Log.e("cantConnectToApi", "No tienes conexión con la API")
        }
        return canConnect
    }

    /**
     * Download all the notes that are not already in the database
     */
    private fun downloadNotesApi() {
        var inserted = false
        if (tryConnection()) {
            runBlocking {
                val corrutina = launch {
                    db = AppDatabase.getDatabase(this@MainActivity)
                    val notesListDB = db.noteDAO().getNotesList() as ArrayList<Note>
                    val notesListApi = CrudApi().getNotesList() as ArrayList<Note>
                    if (notesListApi.size > 0) {
                        for (n in notesListApi) {
                            if (notesListDB.none { it.code == n.code }) {
                                db.noteDAO().insertNote(n)
                                inserted = true
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No hay ninguna nota que descargar",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    if (inserted) {
                        Toast.makeText(
                            this@MainActivity, "Has cargado las notas de la nube", Toast.LENGTH_LONG
                        ).show()
                        val newNotes = db.noteDAO().getNotesList() as ArrayList<Note>

                        fragment =
                            (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!
                        fragment.loadNotes()
                        adapterNotes = AdapterNotes(newNotes, coroutineContext)
                        adapterNotes.updateList(notesListDB)
                    } else {
                        Toast.makeText(
                            this@MainActivity, "No hay notas nuevas en la nube", Toast.LENGTH_LONG
                        ).show()
                    }
                }
                corrutina.join()
            }
        } else {
            Toast.makeText(
                this@MainActivity, "No tienes conexión con la nube", Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Upload all the notes that are not already in the api
     */
    private fun uploadNotesApi() {
        var inserted = false
        if (tryConnection()) {
            runBlocking {
                val corrutina = launch {
                    val notesListDB = db.noteDAO().getNotesList() as ArrayList<Note>
                    val notesListApi = CrudApi().getNotesList() as ArrayList<Note>
                    if (notesListDB.size > 0) {
                        for (n in notesListDB) {
                            if (notesListApi.none { it.code == n.code }) {
                                CrudApi().postNote(n, this@MainActivity)
                                inserted = true
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity, "No tienes ninguna nota que subir", Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

//                    if (inserted) {
////                        webSocketClient.sendMessage("newNote")
//
//                    } else {
//                        Toast.makeText(
//                            this@MainActivity, "No hay notas nuevas que subir", Toast.LENGTH_LONG
//                        ).show()
//                    }
                }
                corrutina.join()
            }
        } else {
            Toast.makeText(
                this@MainActivity, "No tienes conexión con la nube", Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Delete all the notes at the api
     */
    private fun deleteAllNotesApi() {
        if (tryConnection()) {
            var delNotes = CrudApi().getNotesList() as ArrayList<Note>
            if (delNotes.size > 0) {
                for (n in delNotes) {
                    CrudApi().deleteNote(n.code!!)
                }
                Toast.makeText(this, "Has eliminado las notas de la nube", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    this@MainActivity, "No hay ninguna nota en la nube", Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                this@MainActivity, "No tienes conexión con la nube", Toast.LENGTH_LONG
            ).show()
        }
    }

    fun notification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description_text = "Notificaciones del canal"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = description_text
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notifyIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val notifyPendingIntent = PendingIntent.getActivity(
            this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacio =
            NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.app_icon)
                .setContentTitle("Notas").setContentText("Nueva nota").setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Se ha subido una nota nueva a la nube, recarga tus notas y la verás")
                ).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(notifyPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build()

        with(
            NotificationManagerCompat.from(this)
        ) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    notificationPermissionCode
                )
            } else {
                notify(1, notificacio)
                return notificacio
            }
        }
        Toast.makeText(this, "No tiene que llegar hasta aquí", Toast.LENGTH_SHORT).show()
        return notificacio
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == notificationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show()
                with(
                    NotificationManagerCompat.from(this)
                ) {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            notificationPermissionCode
                        )
                    }
                }
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    notificationPermissionCode
                )
            }
        }
    }
}
