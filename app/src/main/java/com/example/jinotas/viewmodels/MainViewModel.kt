package com.example.jinotas.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.jinotas.R
import com.example.jinotas.api.CrudApi
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.utils.UtilsDBAPI.deleteNoteInCloud
import com.example.jinotas.utils.UtilsDBAPI.saveNoteToCloud
import com.example.jinotas.utils.UtilsDBAPI.updateNoteInCloud
import com.example.jinotas.utils.UtilsInternet.isConnectionStableAndFast
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = getApplication<Application>().applicationContext
    private val db: AppDatabase = AppDatabase.getDatabase(application)
    private val _notesCounter = MutableLiveData<String>()
    val notesCounter: LiveData<String> get() = _notesCounter

    /**
     * Here updates the notes counter
     * @param navigationView the navigation view
     */
    fun notesCounter(navigationView: NavigationView) {
        LifecycleService().lifecycleScope.launch {
            val headerView = navigationView.getHeaderView(0)
            val navViewTotalNotes = headerView.findViewById<TextView>(R.id.notesCounter)

            _notesCounter.value = db.noteDAO().getNotesCount()
                .toString() + " " + appContext.getString(R.string.notes_counter)

            navViewTotalNotes.text = notesCounter.value
        }
    }

    /**
     * Sync all notes that have not been inserted, updated or deleted from the API as needed
     * @param userName the username of the logged user
     */
    fun syncPendingNotes(userName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isConnectionStableAndFast(appContext)) {
                val cloudNotes = (CrudApi().getNotesList() as? ArrayList<Note>) ?: arrayListOf()
                val pendingNotes = db.noteDAO().getNotesList().filter { !it.isSynced }
                val localNotes = db.noteDAO().getNotesList() // Lista completa de notas locales

                // Filtrar notas en la nube que pertenecen al usuario actual
                val userCloudNotes = cloudNotes.filter { it.userFrom == userName }

                // Obtener los códigos de notas locales
                val localCodes = localNotes.map { it.code }.toSet()

                // Identificar qué notas en la nube deben eliminarse (las que no existen localmente)
                val notesToDelete = userCloudNotes.filter { it.code !in localCodes }

                for (note in pendingNotes) {
                    try {
                        if (cloudNotes.any { it.code == note.code }) {
                            updateNoteInCloud(note, appContext)
                        } else {
                            saveNoteToCloud(note, appContext)
                        }
                        //Actualiza la nota localmente para saber que está sincronizado
                        note.isSynced = true
                        db.noteDAO().updateNote(note = note)
                        Log.i("Sync", "Nota sincronizada: ${note.title}")
                    } catch (e: Exception) {
                        Log.e("SyncError", "Error al sincronizar la nota: ${note.title}")
                    }
                }

                // Eliminar las notas que están en la nube pero no en las notas locales
                for (note in notesToDelete) {
                    deleteNoteInCloud(note, appContext)
                    Log.e("nota eliminar", note.toString())
                    Log.i("Delete", "Nota eliminada en la nube: ${note.title}")
                }
            }
        }
    }

}