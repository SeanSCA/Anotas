package com.example.jinotas

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import java.util.ArrayList
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapterNotes: AdapterNotes
    private lateinit var notesList: ArrayList<Note>
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
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notesCounter()
        binding.notesCounter.text = notesCounter

        binding.btCreateNote.setOnClickListener {
//            db = AppDatabase.getDatabase(this@MainActivity)
//            val notaExample = Note(null, "nuevo", "tomorrow")
//            db.noteDAO().insertNote(notaExample)
//            notesList = db.noteDAO().getNotes() as ArrayList<Note>
//            adapterNotes = AdapterNotes(notesList!!)
//            adapterNotes.updateList(notesList)
//        }
//        supportFragmentManager.commit {
//            replace<NotesFragment>(R.id.fragment_container_view)
//            setReorderingAllowed(true)
//            addToBackStack(null)
            val intent = Intent(this, WriteNotesActivity::class.java)
            startActivity(intent)
        }
    }
    private fun notesCounter(){
        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(this@MainActivity)
                notesCounter = db.noteDAO().getNotesCount().toString() + " notas"
            }
            corrutina.join()
        }
    }
}