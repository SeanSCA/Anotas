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

    @SerializedName("userFrom") var userFrom: String,

    @SerializedName("userTo") var userTo: String?,

    @SerializedName("CreatedAt") var createdAt: String? = null,

    @SerializedName("UpdatedAt") var updatedAt: String? = null,

    var isSynced: Boolean = true
) {
    constructor(
        id: Int?,
        title: String,
        textContent: String,
        date: String,
        userFrom: String,
        userTo: String?
    ) : this(
        code = UUID.randomUUID().hashCode(),
        id = id,
        title = title,
        textContent = textContent,
        date = date,
        userFrom = userFrom,
        userTo = userTo
    )

    override fun toString(): String {
        return "Note(code=$code, id=$id, title='$title', textContent='$textContent', date='$date', userFrom='$userFrom', userTo=$userTo, createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}