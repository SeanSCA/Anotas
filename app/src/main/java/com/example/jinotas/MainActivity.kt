package com.example.jinotas

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.jinotas.adapter.AdapterNotes
import com.example.jinotas.api.CrudApi
import com.example.jinotas.api.tokenusernocodb.ApiTokenUser
import com.example.jinotas.databinding.ActivityMainBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.db.Token
import com.example.jinotas.utils.Utils.lastClickTime
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.messaging.FirebaseMessaging
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope, SwipeRefreshLayout.OnRefreshListener {
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
    private val PREFS_NAME = "MyPrefsFile"

    // Variable para guardar el nombre de usuario
    private var userName: String? = null

    //Notifications
    private val notificationPermissionCode = 250


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        var instance: MainActivity? = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Firebase.initialize(this)

        instance = this

//        // Solicitar permisos de notificación si no están concedidos
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationPermissionCode
            )
        }

        updateNotesCounter()
        binding.btCreateNote.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 1000) { // Set the minimum click interval to 1 second
                val intent = Intent(this, WriteNotesActivity::class.java)
                intent.putExtra("userFrom", userName)
                startActivity(intent)
            }
            lastClickTime = currentTime
        }

        binding.btSearchNote.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 1000) { // Set the minimum click interval to 1 second
                showPopupMenuSearch(this@MainActivity, binding.btSearchNote)
            }
            lastClickTime = currentTime
        }

        binding.btOrderBy.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 1000) { // Set the minimum click interval to 1 second
                showPopupMenuOrderBy(binding.btOrderBy)
            }
            lastClickTime = currentTime
        }

        // Acceder a las SharedPreferences
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Verificar si es la primera vez que se lanza la aplicación
        val isFirstTime = sharedPreferences.getBoolean("isFirstTime", true)

        if (isFirstTime) {
            // Si es la primera vez, mostrar el formulario
            showFormDialog(sharedPreferences)
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    // Guarda este token en tu base de datos
                    db.tokenDAO().insertNote(Token(token = token))

                    Log.e("Token del dispositivo:", token)
                }
            }
            FirebaseMessaging.getInstance().subscribeToTopic("global")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.e("Topic", "Suscripción exitosa al topic global")
                    } else {
                        Log.e("Topic", "Error al suscribirse al topic")
                    }
                }
        } else {
            // Recuperar el nombre del usuario almacenado en SharedPreferences
            userName = sharedPreferences.getString("userFrom", "")
            // Aquí puedes hacer algo con el nombre de usuario, por ejemplo, mostrarlo en pantalla o usarlo en tu lógica
            Log.e("userNameGuardado", userName!!)

        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.i("recarga", "onRefresh called from SwipeRefreshLayout")

//            fragment =
//                (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!
            fragment.loadNotes()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    // Método para mostrar el formulario en un AlertDialog
    private fun showFormDialog(sharedPreferences: SharedPreferences) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Bienvenido")

        // Crear un Layout para el formulario
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        // Crear el campo del formulario
        val nameInput = EditText(this)
        nameInput.hint = "Nombre de usuario"
        layout.addView(nameInput)

        // Configurar el layout dentro del diálogo
        builder.setView(layout)

        // Botones del diálogo
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            // Guardar el nombre de usuario en una variable
            userName = nameInput.text.toString().lowercase()

            // Guardar el nombre de usuario en SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("isFirstTime", false)  // Marcar que ya no es la primera vez
            editor.putString("userFrom", userName)  // Guardar el nombre de usuario
            editor.apply()

            // Aquí puedes usar la variable userName en la lógica que necesites
            val userToken: ApiTokenUser
            val token = db.tokenDAO().getToken()
            userToken = ApiTokenUser(userName = userName!!, token = token)
            CrudApi().postTokenByUser(userToken)
            dialog.dismiss()
        }

//        builder.setNegativeButton("Cancelar") { dialog, _ ->
//            dialog.cancel()
//        }

        builder.setCancelable(false)
        // Mostrar el diálogo
        builder.show()
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == notificationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    override fun onRefresh() {

    }
}
