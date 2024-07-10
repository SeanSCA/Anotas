package com.example.jinotas.DB

import androidx.room.Query

interface NoteDAO {
    @Query("SELECT * FROM Note")
    fun getNotes(): List<Note>

    @Query("SELECT * FROM Note WHERE id = :id")
    fun getNoteById(id: Int): Note
}