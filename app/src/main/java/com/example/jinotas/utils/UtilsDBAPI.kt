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
        CrudApi.postNote(note)
    }

    //Esto es para modificar en la api
    suspend fun updateNoteInCloud(note: Note, context: Context) {
        CrudApi().patchNote(note)
    }

    //Esto es para eliminar en la api
    suspend fun deleteNoteInCloud(note: Note, context: Context) {
        CrudApi().deleteNote(note.code)
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

    suspend fun deleteNoteInLocalDatabase(note: Note, context: Context) {
        db = AppDatabase.getDatabase(context)
        db.noteDAO().deleteNote(note)
    }
}