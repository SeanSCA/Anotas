package com.example.jinotas.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDAO {
    @Query("SELECT * FROM notes")
    fun getNotesList(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Int): Note

    @Query("SELECT * FROM notes WHERE title  LIKE '%' || :title || '%'")
    fun getNoteByTitle(title: String): List<Note>

    @Query("SELECT * FROM notes ORDER BY date")
    fun getNoteOrderByDate(): List<Note>

    @Query("SELECT * FROM notes ORDER BY title")
    fun getNoteOrderByTitle(): List<Note>

    @Query("SELECT COUNT(*) FROM notes")
    fun getNotesCount(): Int

    @Insert
    fun insertNote(note: Note)

    @Delete
    fun deleteNote(note: Note)

    @Update
    fun updateNote(note: Note)
}