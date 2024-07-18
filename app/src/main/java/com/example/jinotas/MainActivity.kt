package com.example.jinotas

import android.content.Context
import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.ActivityMainBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.ArrayList
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var adapterNotes: AdapterNotes
    private var notesCounter: String? = null
    private var job: Job = Job()
    private lateinit var fragment: NotesFragment
    private var canConnect: Boolean = false

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


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
     * Here updates the notes counter from the api
     */
    private fun notesCounterApi() {
        notesCounter = CrudApi().getNotesList()?.size.toString()
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

//        searchNote.afterTextChanged {
//
//        }
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
    private fun tryConnection(): Boolean {
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
        if (tryConnection()) {
            runBlocking {
                val corrutina = launch {

                    db = AppDatabase.getDatabase(this@MainActivity)
                    val notesListDB = db.noteDAO().getNotesList() as ArrayList<Note>
                    val notesListApi = CrudApi().getNotesList() as ArrayList<Note>
                    for (n in notesListDB) {
                        if (notesListApi.contains(n)) {
                            Toast.makeText(this@MainActivity, n.title, Toast.LENGTH_LONG).show()
                        }
                    }
//                    adapterNotes = AdapterNotes(notesListDB, coroutineContext)
//                    adapterNotes.updateList(notesListDB)
                }
                corrutina.join()
            }
        }
    }

}