package com.example.jinotas.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    @SerializedName("Id") // Campo del JSON "Id"
    val id: Int = UUID.randomUUID().hashCode(),

    @SerializedName("title") // Campo del JSON "title"
    var title: String,

    @SerializedName("textContent") // Campo del JSON "textContent"
    var textContent: String,

    @SerializedName("date") // Campo del JSON "date"
    var date: String,

    @SerializedName("CreatedAt") // Campo del JSON "CreatedAt"
    var createdAt: String? = null, // Opcional: puedes ignorarlo si no lo necesitas

    @SerializedName("UpdatedAt") // Campo del JSON "UpdatedAt"
    var updatedAt: String? = null // Opcional: puedes ignorarlo si no lo necesitas
) {
    constructor(title: String, content: String, date: String) : this(
        UUID.randomUUID().hashCode(), title, content, date
    )
}
