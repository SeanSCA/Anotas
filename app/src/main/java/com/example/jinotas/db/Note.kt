package com.example.jinotas.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.ArrayList
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var title: String,
    var textContent: String,
    var date: String
) {
    constructor(title: String, content: String, date: String) : this(
        UUID.randomUUID().toString(), title, content, date
    )
}

//var notes : ArrayList<Note> = arrayListOf()
//var notesArrayList = ArrayList<Note>()