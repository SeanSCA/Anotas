package com.example.jinotas.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface NoteDAO {
    @Query("SELECT * FROM notes")
    fun getNotesList(): List<Note>

    @Query("SELECT * FROM notes WHERE code = :code")
    fun getNoteByCode(code: Int): Note

    @Query("SELECT * FROM notes WHERE title  LIKE '%' || :title || '%'")
    fun getNoteByTitle(title: String): List<Note>

    @Query("SELECT * FROM notes ORDER BY date")
    fun getNoteOrderByDate(): List<Note>

    @Query("SELECT * FROM notes ORDER BY title")
    fun getNoteOrderByTitle(): List<Note>

    @Query("SELECT COUNT(*) FROM notes")
    fun getNotesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNote(note: Note)

    @Delete
    fun deleteNote(note: Note)

    @Update
    fun updateNote(note: Note)

    @Transaction
    suspend fun deleteNoteWithTransaction(note: Note) {
        try {
            deleteNote(note)
        } catch (e: Exception) {
            throw Exception("Error eliminando la nota: ${e.message}")
        }
    }
}