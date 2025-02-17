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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.jinotas.adapter.AdapterNotes
import com.example.jinotas.api.CrudApi
import com.example.jinotas.connection.ConnectivityMonitor
import com.example.jinotas.databinding.ActivityMainBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.db.Token
import com.example.jinotas.db.UserToken
import com.example.jinotas.utils.Utils
import com.example.jinotas.utils.Utils.getJsonFromAssets
import com.example.jinotas.utils.Utils.masterKeyAlias
import com.example.jinotas.utils.Utils.vibratePhone
import com.example.jinotas.utils.UtilsDBAPI.deleteNoteInCloud
import com.example.jinotas.utils.UtilsDBAPI.saveNoteToCloud
import com.example.jinotas.utils.UtilsDBAPI.updateNoteInCloud
import com.example.jinotas.utils.UtilsInternet.checkConnectivity
import com.example.jinotas.utils.UtilsInternet.isConnectedToInternet
import com.example.jinotas.utils.UtilsInternet.isConnectionStableAndFast
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.messaging.FirebaseMessaging
import com.muddassir.connection_checker.ConnectionChecker
import com.muddassir.connection_checker.ConnectionState
import com.muddassir.connection_checker.ConnectivityListener
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope, SwipeRefreshLayout.OnRefreshListener,
    ConnectivityListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var adapterNotes: AdapterNotes
    private var notesCounter: String? = null
    private var job: Job = Job()
    private lateinit var fragmentNotes: NotesFragment
    private var canConnect: Boolean = false
    lateinit var drawerLayout: DrawerLayout
    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var expandableListView: ExpandableListView
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

        //Esto es para comprobar la conexión al iniciar la aplicación y subir las notas guardadas únicamente en local
        val connectivityMonitor = ConnectivityMonitor(applicationContext)
        connectivityMonitor.registerCallback {
            syncPendingNotes()
        }

        //Esto es para comprobar la conexión al cambiar la conexión a internet o recuperarla y subir las notas guardadas únicamente en local
        val connectionChecker = ConnectionChecker(this)
        connectionChecker.connectivityListener = this

        //Esto es para el menu desplegable
        drawerLayout = binding.myDrawerLayout
        val navigationView = binding.navigationView
        navigationView.setItemTextAppearance(R.style.AldrichTextViewStyle)
        expandableListView = binding.expandableListView
        val toolbar: Toolbar = binding.toolbar

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close
        )

        toggle.isDrawerIndicatorEnabled = false  // Deshabilita el icono predeterminado
        toggle.setHomeAsUpIndicator(R.drawable.return_to_notes) //Coloca un icono personalizado

        drawerLayout.addDrawerListener(toggle)

        toggle.syncState()

        //Listener para abrir y cerrar drawer
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)

            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        Utils.setupExpandableListView(
            expandableListView, this, drawerLayout
        )

        drawerLayout.closeDrawer(GravityCompat.START)
        //Hasta aqui


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
            vibratePhone(this)
            val intent = Intent(this, WriteNotesActivity::class.java)
            intent.putExtra("userFrom", userName)
            startActivity(intent)
        }

        binding.btSearchNote.setOnClickListener {
            showPopupMenuSearch(this@MainActivity, binding.btSearchNote)
        }

        binding.btOrderBy.setOnClickListener {
            showPopupMenuOrderBy(binding.btOrderBy)
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
                    db.tokenDAO().insertToken(Token(token = token))

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
            lifecycleScope.launch {
                Utils.saveValues("Vertical", this@MainActivity)
            }

            val secretSharedPreferences = EncryptedSharedPreferences.create(
                "secure_shared_prefs",
                masterKeyAlias,
                applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val jsonString = getJsonFromAssets(applicationContext)

            if (jsonString != null) {
                secretSharedPreferences.edit().putString("firebase_json", jsonString).apply()
            } else {
                println("❌ Error al cargar el archivo JSON")
            }
        } else {
            userName = sharedPreferences.getString("userFrom", "")
            Log.e("userNameGuardado", userName!!)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.i("recarga", "onRefresh called from SwipeRefreshLayout")

            fragmentNotes.loadNotes()
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

            lifecycleScope.launch {
                val userToken: UserToken
                val token = db.tokenDAO().getToken()
                userToken = UserToken(token = token, userName = userName!!, password = "")

                CrudApi().postTokenByUser(userToken)
            }

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
        fragmentNotes =
            (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!
        if (::db.isInitialized) {
            db.noteDAO().getAllNotesLive().observe(this) { notes ->
                fragmentNotes.loadNotes()
            }
        }
    }

    /**
     * Here updates the notes counter
     */
    private fun notesCounter() {
        lifecycleScope.launch {
            db = AppDatabase.getDatabase(this@MainActivity)
            notesCounter = db.noteDAO().getNotesCount()
                .toString() + " " + applicationContext.getString(R.string.notes_counter)

            binding.notesCounter.text = notesCounter
        }
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

        val searchNote = layout.findViewById<EditText>(R.id.etSearchNote)

        val popup = PopupWindow(
            layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )

        popup.isOutsideTouchable = true
        popup.isTouchable = true


        fragmentNotes =
            (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!
        fragmentNotes.apply {
            searchNote.afterTextChanged {
                loadFilteredNotes(searchNote.text.toString())
            }
        }

        popup.showAsDropDown(view)
    }

    /**
     * shows a popup with a few options to order the notes
     * @param view The view to anchor the popup
     */
    fun showPopupMenuOrderBy(view: View) {
        fragmentNotes =
            (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!

        val inflater = LayoutInflater.from(applicationContext)
        val layout = inflater.inflate(R.layout.menu_order_by, null)


        val popup = PopupWindow(
            layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )

        popup.isOutsideTouchable = true
        popup.isTouchable = true

        popup.showAsDropDown(view)
        val orderByDate = layout.findViewById<Button>(R.id.action_order_by_date)
        val orderByTitle = layout.findViewById<Button>(R.id.action_order_by_title)

        orderByDate.setOnClickListener {
            fragmentNotes.orderByNotes("date")
            popup.dismiss()
        }

        orderByTitle.setOnClickListener {
            fragmentNotes.orderByNotes("title")
            popup.dismiss()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        // Maneja el clic en el botón de hamburguesa
        return if (item.itemId == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
        Log.i("OnBackPressed", "Has pulsado retroceder")
    }

    override fun onConnectionState(state: ConnectionState) {
        try {
            isConnectedToInternet = checkConnectivity(state, applicationContext)
            Log.i("isConnectedToInternet", isConnectedToInternet.toString())
            if (isConnectedToInternet != null) {
                syncPendingNotes()
            }
        } catch (e: Exception) {
            Log.e("isConnectedToInternet", "No se puede comprobar si está conectado")
        }
    }

    private fun syncPendingNotes() {
        db = AppDatabase.getDatabase(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            if (isConnectionStableAndFast(applicationContext)) {
                val cloudNotes = (CrudApi().getNotesList() as? ArrayList<Note>) ?: arrayListOf()
                val pendingNotes = db.noteDAO().getNotesList().filter { !it.isSynced }
                val localNotes = db.noteDAO().getNotesList() // Lista completa de notas locales

                // Filtrar notas en la nube que pertenecen al usuario actual
                val userCloudNotes = cloudNotes.filter { it.userFrom == userName }

                // Obtener los códigos de notas locales
                val localCodes = localNotes.map { it.code }.toSet()

                // Identificar qué notas en la nube deben eliminarse (las que no existen localmente)
                val notesToDelete = userCloudNotes.filter { it.code !in localCodes }

                for (note in pendingNotes) {
                    try {
                        if (cloudNotes.any { it.code == note.code }) {
                            updateNoteInCloud(note, applicationContext)
                        } else {
                            saveNoteToCloud(note, applicationContext)
                        }
                        //Actualiza la nota localmente para saber que está sincronizado
                        note.isSynced = true
                        db.noteDAO().updateNote(note = note)
                        Log.i("Sync", "Nota sincronizada: ${note.title}")
                    } catch (e: Exception) {
                        Log.e("SyncError", "Error al sincronizar la nota: ${note.title}")
                    }
                }

                // Eliminar las notas que están en la nube pero no en las notas locales
                for (note in notesToDelete) {
                    deleteNoteInCloud(note, applicationContext)
                    Log.e("nota eliminar", note.toString())
                    Log.i("Delete", "Nota eliminada en la nube: ${note.title}")
                }
            }
        }
    }
}
