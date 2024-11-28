package com.example.jinotas.utils

import android.content.Context
import com.example.jinotas.api.CrudApi
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note

object UtilsDBAPI {
    private val CrudApi = CrudApi()
    private lateinit var db: AppDatabase

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
}