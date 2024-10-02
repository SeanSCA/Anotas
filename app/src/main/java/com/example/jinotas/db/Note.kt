package com.example.jinotas.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey @SerializedName("code") val code: Int = UUID.randomUUID().hashCode(),

    @SerializedName("Id") var id: Int?,

    @SerializedName("title") var title: String,

    @SerializedName("textContent") var textContent: String,

    @SerializedName("date") var date: String,

    @SerializedName("CreatedAt") var createdAt: String? = null,

    @SerializedName("UpdatedAt") var updatedAt: String? = null
) {
    constructor(id: Int?, title: String, textContent: String, date: String) : this(
        code = UUID.randomUUID().hashCode(),
        id = id,
        title = title,
        textContent = textContent,
        date = date
    )

    override fun toString(): String {
        return "Note(code=$code, id=$id, title='$title', textContent='$textContent', date='$date', createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}