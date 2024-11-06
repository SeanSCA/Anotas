package com.example.jinotas

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.ActivityShowNoteBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.utils.Utils.addCheckbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext

class ShowNoteActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityShowNoteBinding
    private lateinit var notesShow: Note
    private lateinit var db: AppDatabase
    private var job: Job = Job()
    private var focusedEditText: EditText? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowNoteBinding.inflate(layoutInflater)
        val codeSearchUpdate = intent.getIntExtra("code", 0)
        val userName = intent.getStringExtra("userFrom")

        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(this@ShowNoteActivity)
                notesShow = db.noteDAO().getNoteByCode(codeSearchUpdate)
                notesShow.let {
                    val markdownContent =
                        it.textContent  // Obtener el contenido en formato Markdown
                    binding.etTitle.setText(notesShow.title)
                }
            }
            corrutina.join()
        }

        binding.btReturnToNotes.setOnClickListener {
            finish()
        }

//        binding.btOverwriteNote.setOnClickListener {
//            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//            val current = LocalDateTime.now().format(formatter)
//
//            // Obtener el contenido completo en Markdown del WebView
//            getMarkdownFromWebView { markdownContent ->
//                runBlocking {
//                    val corrutina = launch {
//                        val noteUpdate = Note(
//                            codeSearchUpdate,
//                            notesShow.id,
//                            binding.etTitle.text.toString(),
//                            markdownContent,
//                            current,
//                            userName!!,
//                            null,
//                            null
//                        )
//                        Log.i("notaUpdate", noteUpdate.toString())
//                        db.noteDAO().updateNote(noteUpdate)
//                        CrudApi().patchNote(noteUpdate)
//                        Toast.makeText(
//                            this@ShowNoteActivity, "Has modificado la nota", Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                    corrutina.join()
//                }
//                finish()
//            }
//        }

//        binding.btAddCheckbox.setOnClickListener {
//            addCheckbox(binding.textViewMarkdown)
//        }

        setContentView(binding.root)
    }

//    private fun setupWebView() {
//        val webView = binding.textViewMarkdown
//        webView.settings.javaScriptEnabled = true
//        webView.webViewClient = object : WebViewClient() {
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//                // Cargar el contenido en el WebView después de cargar la página
//                loadMarkdownIntoWebView(notesShow.textContent)
//            }
//        }
//        webView.loadUrl("file:///android_asset/markdown_editor.html")
//    }
//
//    private fun getMarkdownFromWebView(onMarkdownReceived: (String) -> Unit) {
//        binding.textViewMarkdown.evaluateJavascript("getMarkdownContent()") { value ->
//            // Limpiar el contenido HTML obtenido
//            val markdownContent = value.removeSurrounding("\"")
//                .replace("&nbsp;", " ") // Reemplazar espacios no rompibles
//                .replace("\\u003C", "<") // Reemplazar caracteres escapados
//                .replace("\\u003E", ">") // Reemplazar caracteres escapados
//                .trim() // Limpiar espacios en blanco
//            Log.i("MarkdownContent", "Contenido obtenido del WebView: $markdownContent") // Verificar el formato
//            onMarkdownReceived(markdownContent)
//        }
//    }
//
//    private fun loadMarkdownIntoWebView(markdownContent: String) {
//        // Asegúrate de que el contenido no contenga líneas vacías o caracteres no deseados
//        val formattedContent = markdownContent.trim().replace("\n", "\n") // Conservar saltos de línea
//        Log.i("MarkdownContent", "Cargando en WebView: $formattedContent") // Log para verificar
//        binding.textViewMarkdown.evaluateJavascript("initializeMarkdown('$formattedContent')", null)
//    }
}
