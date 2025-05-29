package com.example.jinotas.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.jinotas.utils.SyncStatus
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey @SerializedName("code") val code: Int = UUID.randomUUID().hashCode(),

    @SerializedName("title") var title: String = "",

    @SerializedName("textContent") var textContent: String = "",

    @SerializedName("date") var date: String = ""
) {
    constructor(
        title: String, textContent: String, date: String
    ) : this(
        code = UUID.randomUUID().hashCode(), title = title, textContent = textContent, date = date
    )

    override fun toString(): String {
        return "Note(code=$code, title='$title', textContent='$textContent', date='$date')"
    }

}