package com.example.jinotas

import android.Manifest
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import android.widget.PopupWindow
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.jinotas.connection.ConnectivityMonitor
import com.example.jinotas.databinding.ActivityMainBinding
import com.example.jinotas.utils.Utils
import com.example.jinotas.utils.Utils.vibratePhone
import com.example.jinotas.utils.UtilsInternet.checkConnectivity
import com.example.jinotas.utils.UtilsInternet.isConnectedToInternet
import com.example.jinotas.viewmodels.MainViewModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.muddassir.connection_checker.ConnectionChecker
import com.muddassir.connection_checker.ConnectionState
import com.muddassir.connection_checker.ConnectivityListener
import androidx.core.content.edit

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    ConnectivityListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationView: NavigationView
    private lateinit var fragmentNotes: NotesFragment
    lateinit var drawerLayout: DrawerLayout
    private lateinit var expandableListView: ExpandableListView
    private val PREFS_NAME = "MyPrefsFile"

    private lateinit var mainViewModel: MainViewModel

    // Variable para guardar el nombre de usuario
//    private var userName: String? = null

    //Notifications
    private val PermissionCode = 250

    companion object {
        var instance: MainActivity? = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        //Esto es para comprobar la conexión al iniciar la aplicación y subir las notas guardadas únicamente en local
        val connectivityMonitor = ConnectivityMonitor(applicationContext)

        //Esto es para comprobar la conexión al cambiar la conexión a internet o recuperarla y subir las notas guardadas únicamente en local
        val connectionChecker = ConnectionChecker(this)
        connectionChecker.connectivityListener = this

        //Esto es para el menu desplegable
        drawerLayout = binding.myDrawerLayout
        navigationView = binding.navigationView
        navigationView.post {
            val headerView = navigationView.getHeaderView(0)

            val headerHeight = headerView.height

            expandableListView.setPadding(0, headerHeight, 0, 0)
        }

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

        instance = this

//        // Solicitar permisos de notificación si no están concedidos
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION
                ), PermissionCode
            )
        }

        updateNotesCounter()
        binding.btCreateNote.setOnClickListener {
            vibratePhone(this)
            val intent = Intent(this, WriteNotesActivity::class.java)

            val options = ActivityOptions.makeCustomAnimation(
                applicationContext, R.anim.fade_in, R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
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
            mainViewModel.saveNoteListStyle("Vertical", applicationContext)
            Log.i("isFirstTime", "isFirstTime iniciado a vertical")
            sharedPreferences.edit { putBoolean("isFirstTime", false) }
        }

        connectivityMonitor.registerCallback {
            mainViewModel.syncPendingNotes()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.i("recarga", "onRefresh called from SwipeRefreshLayout")
            mainViewModel.syncPendingNotes()
            fragmentNotes.loadNotes()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    // Método para mostrar el formulario en un AlertDialog
//    private fun showFormDialog(sharedPreferences: SharedPreferences) {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Bienvenido")
//
//        // Crear un Layout para el formulario
//        val layout = LinearLayout(this)
//        layout.orientation = LinearLayout.VERTICAL
//
//        // Crear el campo del formulario
//        val nameInput = EditText(this).apply {
//            hint = "Nombre de usuario"
//            inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
//        }
//        layout.addView(nameInput)
//
//        // Configurar el layout dentro del diálogo
//        builder.setView(layout)
//
//        // Botones del diálogo
//        builder.setPositiveButton("Aceptar") { dialog, _ ->
//            // Guardar el nombre de usuario en una variable
//            userName = nameInput.text.toString().lowercase()
//
//            mainViewModel.saveUserToken(userName!!, sharedPreferences)
//
//            val headerView = navigationView.getHeaderView(0) // Esto obtiene la vista del encabezado
//
//            val navViewUserName = headerView.findViewById<TextView>(R.id.nav_username)
//            navViewUserName.text = userName
//
//            dialog.dismiss()
//        }
//
//
//        builder.setCancelable(false)
//        // Mostrar el diálogo
//        builder.show()
//    }

    /**
     * Here it reloads all the notes when the app returns to this activity
     */
    override fun onResume() {
        super.onResume()
        fragmentNotes =
            (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!

        mainViewModel.getAllNotesLive()!!.observe(this) {
            fragmentNotes.loadNotes()
        }

    }

    /**
     * Here updates the notes counter every 0.5 seconds
     */
    private fun updateNotesCounter() {
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                mainViewModel.notesCounter(navigationView)
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

        if (requestCode == PermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                with(
                    NotificationManagerCompat.from(this)
                ) {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity, arrayOf(
                                Manifest.permission.POST_NOTIFICATIONS,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ), PermissionCode
                        )
                    }
                }
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), PermissionCode
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
//                mainViewModel.syncPendingNotes(userName!!)
                mainViewModel.syncPendingNotes()
            }
        } catch (e: Exception) {
            Log.e("isConnectedToInternet", "No se puede comprobar si está conectado")
        }
    }
}
