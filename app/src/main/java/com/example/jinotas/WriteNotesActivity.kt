package com.example.jinotas

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.commonsware.cwac.anddown.AndDown
import com.example.jinotas.adapter.AdapterNotes
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.ActivityWriteNotesBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.utils.Utils
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
    private lateinit var andDown: AndDown
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

        val userNameFrom = intent.getStringExtra("userFrom")
        andDown = AndDown()
        setupWebView()

        binding.btReturnToNotes.setOnClickListener {
            finish()
        }

        binding.btSaveNote.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val current = LocalDateTime.now().format(formatter)

            // Obtener el contenido Markdown del WebView
            getMarkdownFromWebView { markdownContent ->
                Log.i("MarkdownToSave", "Markdown que se guardará: $markdownContent") // Verifica el contenido antes de guardar

                runBlocking {
                    val corrutina = launch {
                        val note = Note(
                            id = null,
                            title = binding.etTitle.text.toString(),
                            textContent = markdownContent, // Guardar el Markdown en textContent
                            date = current.toString(),
                            userFrom = userNameFrom ?: "",
                            userTo = null
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


        binding.btAddCheckbox.setOnClickListener {
            Log.i("btAddCheckbox", "Has pulsado el botón addCheckbox")
            Utils.addCheckbox(binding.textViewMarkdown)
        }
    }

    private fun setupWebView() {
        val webView = binding.textViewMarkdown
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("file:///android_asset/markdown_editor.html")
    }


    private fun getMarkdownFromWebView(onMarkdownReceived: (String) -> Unit) {
        binding.textViewMarkdown.evaluateJavascript("getMarkdownContent()") { value ->
            // Limpiar el contenido HTML obtenido
            val markdownContent = value.removeSurrounding("\"")
                .replace("&nbsp;", " ") // Reemplazar espacios no rompibles
                .replace("\\u003C", "<") // Reemplazar caracteres escapados
                .replace("\\u003E", ">") // Reemplazar caracteres escapados
                .trim() // Limpiar espacios en blanco
            Log.i("MarkdownContent", "Contenido obtenido del WebView: $markdownContent") // Verificar el formato
            onMarkdownReceived(markdownContent)
        }
    }



    private fun uploadNoteApi(notePost: Note) {
        if (tryConnection()) {
            runBlocking {
                val corrutina = launch {
                    CrudApi().postNote(notePost, this@WriteNotesActivity)
                    Toast.makeText(this@WriteNotesActivity, "Has subido la nota", Toast.LENGTH_LONG).show()
                }
                corrutina.join()
            }
        }
    }

    fun tryConnection(): Boolean {
        try {
            canConnect = CrudApi().canConnectToApi()
        } catch (e: Exception) {
            Log.e("cantConnectToApi", "No tienes conexión con la API")
        }
        return canConnect
    }
}
