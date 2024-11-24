package com.example.jinotas.utils

import android.content.Context
import com.example.jinotas.api.CrudApi
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note

object UtilsDBAPI {
    private val CrudApi = CrudApi()
    private lateinit var db: AppDatabase

    suspend fun saveNoteToLocalDatabase(note: Note, context: Context) {
        // Aquí almacenas la nota en Room
        CrudApi.postNote(note, context)
    }

    suspend fun saveNoteToCloud(note: Note, context: Context) {
        // Aquí almacenas la nota en NocoDB mediante una API
        db = AppDatabase.getDatabase(context)
        db.noteDAO().insertNote(note)
    }

}