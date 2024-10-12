package com.example.jinotas

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.ActivityWriteNotesBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext


class WriteNotesActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityWriteNotesBinding
    private lateinit var notesList: ArrayList<Note>
    private lateinit var adapterNotes: AdapterNotes
    private lateinit var db: AppDatabase
    private var job: Job = Job()
    private var canConnect: Boolean = false

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var userName = intent.getStringExtra("user")

        binding.btReturnToNotes.setOnClickListener {
            finish()
        }

        binding.btSaveNote.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val current = LocalDateTime.now().format(formatter)
            runBlocking {
                val corrutina = launch {
                    val note = Note(
                        null,
                        binding.etTitle.text.toString(),
                        binding.etNoteContent.text.toString(),
                        current.toString(),
                        userName!!
                    )
                    db = AppDatabase.getDatabase(this@WriteNotesActivity)
                    db.noteDAO().insertNote(note)
                    notesList = db.noteDAO().getNotesList() as ArrayList<Note>
                    adapterNotes = AdapterNotes(notesList, coroutineContext)
                    adapterNotes.updateList(notesList)
                    uploadNoteApi(note)
                }
                corrutina.join()
            }
            finish()
        }
    }

    private fun uploadNoteApi(notePost: Note) {
        if (tryConnection()) {
            runBlocking {
                val corrutina = launch {
                    CrudApi().postNote(notePost, this@WriteNotesActivity)
                    Toast.makeText(this@WriteNotesActivity, "Has subido la nota", Toast.LENGTH_LONG)
                        .show()
                }
                corrutina.join()
            }
        }
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
}