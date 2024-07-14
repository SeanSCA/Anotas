package com.example.jinotas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.jinotas.databinding.ActivityMainBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.ArrayList
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private var notesCounter: String? = null
    private var job: Job = Job()

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
            showPopupMenu(this@MainActivity, binding.btSearchNote)
        }
    }

    override fun onResume() {
        super.onResume()
        val fragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment
        fragment?.loadNotes()
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

    fun showPopupMenu(context: Context, view: View) {
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

        // display the popup window at the specified location
        popup.showAsDropDown(view)
    }
}