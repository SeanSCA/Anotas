package com.example.jinotas.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jinotas.adapter.AdapterNotesWidget
import com.example.jinotas.databinding.ActivityConfigBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.launch

class ConfigActivity : AppCompatActivity() {
    private var appWidgetId = 0
    private lateinit var db: AppDatabase
    private lateinit var adapterNotes: AdapterNotesWidget
    private lateinit var binding: ActivityConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(RESULT_CANCELED)
        handleSetupWidget();
    }


    private fun handleSetupWidget() {
        // Obtener el ID del widget
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // Si el ID del widget no es válido, cerrar la actividad
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Mostrar la lista de notas
        loadNotes()
    }

    // Cargar notas desde la base de datos
    private fun loadNotes() {
        lifecycleScope.launch {
            db = AppDatabase.getDatabase(applicationContext)
            val notes = db.noteDAO().getNotesList() as ArrayList<Note>
            adapterNotes = AdapterNotesWidget(notes, coroutineContext) { note ->
                saveSelectedNoteAndClose(note)
            }
            binding.rvNotesWidget.layoutManager = LinearLayoutManager(applicationContext)
            binding.rvNotesWidget.adapter = adapterNotes
        }
    }

    // Guardar la nota seleccionada y cerrar
    private fun saveSelectedNoteAndClose(note: Note) {
        val sharedPreferences = getSharedPreferences("WidgetPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("widget_note_${appWidgetId}_code", note.code.toString())
            .putString("widget_note_${appWidgetId}_title", note.title)
            .putString("widget_note_${appWidgetId}_content", note.textContent).apply()

        // Actualizar el widget con la nueva nota
        val appWidgetManager = AppWidgetManager.getInstance(this)
        WidgetProvider.updateWidget(
            this, appWidgetManager, appWidgetId, note.title, note.textContent
        )

        // Devolver el resultado a AppWidgetManager
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)

        finish()
    }


}