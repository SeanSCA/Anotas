package com.example.jinotas.DB

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.ArrayList
import java.util.Date

@Entity
data class Note(
    @PrimaryKey
    var id: Int,
    var textContext: String,
    var date : Date
)

//var notes : ArrayList<Note> = arrayListOf()
var notes = ArrayList<Note>()