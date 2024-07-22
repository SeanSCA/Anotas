package com.example.jinotas.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.ArrayList
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: Int = UUID.randomUUID().hashCode(),
    var title: String,
    var textContent: String,
    var date: String
) {
    constructor(title: String, content: String, date: String) : this(
        UUID.randomUUID().hashCode(), title, content, date
    )
}
