package com.example.jinotas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private var notesCounter: String? = null
    private var job: Job = Job()
    private lateinit var fragment: NotesFragment

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

    override fun onResume() {
        super.onResume()
        fragment =
            (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment)!!
        fragment.loadNotes()
    }


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

    private fun updateNotesCounter() {
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                notesCounter()
                mainHandler.postDelayed(this, 500)
            }
        })
    }

    private fun  showPopupMenuSearch(context: Context, view: View) {
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

}