package com.example.jinotas.utils

import android.content.Context
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.jinotas.api.CrudApi
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object UtilsDBAPI {
    private val CrudApi = CrudApi()
    private lateinit var db: AppDatabase
    lateinit var localPendingNotes: MutableList<Note>
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")    // Método para obtener el ID del dispositivo
    val FILE = stringPreferencesKey("notes_list_style")
    //Esto es para almacenar en la api
    suspend fun saveNoteToCloud(note: Note, context: Context) {
        CrudApi.postNote(note, context)
    }

    //Esto es para modificar en la api
    suspend fun updateNoteInCloud(note: Note, context: Context) {
        CrudApi().patchNote(note)
    }

    //-------------------

    //Esto es para almacenar en la DB
    suspend fun saveNoteToLocalDatabase(note: Note, context: Context) {
        // Aquí almacenas la nota en NocoDB mediante una API
        db = AppDatabase.getDatabase(context)
        db.noteDAO().insertNote(note)
    }

    //Esto es para modificar en la DB
    suspend fun updateNoteInLocalDatabase(note: Note, context: Context) {
        db = AppDatabase.getDatabase(context)
        db.noteDAO().updateNote(note)
    }

    fun saveNoteApiWhenRecoverInternet(note: Note, localPendingNotes: MutableList<Note>, context: Context) {
        // Implement your logic to store the note locally
        //Notas de la api
        val notesApi = CrudApi().getNotesList()
        localPendingNotes.add(note) // Assuming localPendingNotes is a mutable list of Note
        notesApi?.forEach { cloud ->
            if (cloud != null && note != null && cloud.code != note.code) {
                CrudApi().postNote(note, context)
            }
        }
    }

    /**
     * Esta función es para almacenar la nota localmente y en la api en el caso de tener internet
     */
    suspend fun saveNoteConcurrentlyWithInternet(note: Note, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val localSave = async { saveNoteToLocalDatabase(note, context) }
            val cloudSave = async { saveNoteToCloud(note, context) }

            try {
                // Espera a que ambas operaciones terminen
                localSave.await()
                cloudSave.await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Has creado la nota ${note.title}", Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Manejo de errores
                    Toast.makeText(
                        context, "Error al guardar la nota", Toast.LENGTH_SHORT
                    ).show()
                }
                saveNoteApiWhenRecoverInternet(note, mutableListOf(note), context)
            }
        }
    }

    /**
     * Esta función es para almacenar la nota localmente en el caso de no tener internet
     */
    suspend fun saveNoteConcurrentlyWithoutInternet(note: Note, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
//            val localSave = async { saveNoteToLocalDatabase(note, context) }

            try {
                // Espera a que ambas operaciones terminen
//                localSave.await()
                saveNoteToLocalDatabase(note, context)
                saveNoteApiWhenRecoverInternet(note, mutableListOf(note), context)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Has creado la nota ${note.title}", Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Manejo de errores
                    Toast.makeText(
                        context, "Error al guardar la nota", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    suspend fun saveNoteLocallyForLaterSync(note: Note, context: Context) {
        // Add logic to flag the note as unsynced in your local database
        saveNoteApiWhenRecoverInternet(note, localPendingNotes, context)
        removePendingNote(note)
    }

    fun addNoteToPendingNotes(note: Note){
        localPendingNotes.add(note)
    }

    fun getPendingNotes(context: Context): MutableList<Note> {
        // Implement your logic to retrieve the pending notes from local storage
        var db = AppDatabase.getDatabase(context)
        localPendingNotes = db.noteDAO().getNotesList() as MutableList<Note>
        return localPendingNotes // Or fetch from database/shared preferences
    }

    fun removePendingNote(note: Note) {
        // Implement your logic to remove the note from local storage
        localPendingNotes.remove(note) // Or delete from the database
    }
}