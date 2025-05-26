package com.example.jinotas.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.jinotas.utils.SyncStatus
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey @SerializedName("code") val code: Int = UUID.randomUUID().hashCode(),

    @SerializedName("Id") var id: Int?,

    @SerializedName("title") var title: String,

    @SerializedName("textContent") var textContent: String,

    @SerializedName("date") var date: String,

//    @SerializedName("userFrom") var userFrom: String,

//    @SerializedName("userTo") var userTo: String?,

    @SerializedName("updatedTime") var updatedTime: Long,

    var syncStatus: SyncStatus = SyncStatus.SYNCED,

    var isSynced: Boolean = true
) {
    constructor(
        id: Int?,
        title: String,
        textContent: String,
        date: String,
//        userFrom: String,
//        userTo: String?,
        updatedTime: Long
    ) : this(
        code = UUID.randomUUID().hashCode(),
        id = id,
        title = title,
        textContent = textContent,
        date = date,
//        userFrom = userFrom,
//        userTo = userTo,
        updatedTime = updatedTime
    )

    override fun toString(): String {
//        return "Note(id= $id, code=$code, title='$title', textContent='$textContent', date='$date', userFrom='$userFrom', userTo=$userTo, isSynced='$isSynced'"
        return "Note(id= $id, code=$code, title='$title', textContent='$textContent', date='$date', isSynced='$isSynced'"
    }
}