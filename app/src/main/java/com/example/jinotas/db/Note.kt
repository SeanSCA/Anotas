package com.example.jinotas.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey @SerializedName("code") val code: Int = UUID.randomUUID().hashCode(),

    @SerializedName("title") var title: String,

    @SerializedName("textContent") var textContent: String,

    @SerializedName("date") var date: String,

    @SerializedName("userFrom") var userFrom: String,

    @SerializedName("userTo") var userTo: String?,

    var isSynced: Boolean = true
) {
    constructor(
        title: String,
        textContent: String,
        date: String,
        userFrom: String,
        userTo: String?
    ) : this(
        code = UUID.randomUUID().hashCode(),
        title = title,
        textContent = textContent,
        date = date,
        userFrom = userFrom,
        userTo = userTo
    )

    override fun toString(): String {
        return "Note(code=$code, title='$title', textContent='$textContent', date='$date', userFrom='$userFrom', userTo=$userTo"
    }
}