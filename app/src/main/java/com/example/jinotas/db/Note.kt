package com.example.jinotas.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.ArrayList

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey (autoGenerate = true)
    val id: Int?,
    val title: String,
    val textContent: String,
    val date : String
)

//var notes : ArrayList<Note> = arrayListOf()
var notes = ArrayList<Note>()