package com.example.jinotas

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.jinotas.databinding.ActivityShowNoteBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.ArrayList
import kotlin.coroutines.CoroutineContext

class ShowNoteActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityShowNoteBinding
    private lateinit var notesShow: Note
    private lateinit var db: AppDatabase
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowNoteBinding.inflate(layoutInflater)
//        enableEdgeToEdge()
        var idSearch = intent.getIntExtra("id", 0)
        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(this@ShowNoteActivity)
                notesShow = db.noteDAO().getNoteById(idSearch)
            }
            corrutina.join()
        }

        binding.etTitle.setText(notesShow.title)
        binding.etNoteContent.setText(notesShow.textContext)
        setContentView(binding.root)
    }
}