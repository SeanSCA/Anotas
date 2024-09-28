package com.example.jinotas.api.notesnocodb

import com.google.gson.annotations.SerializedName

data class ApiNote(
    @SerializedName("Id") val id: Int,

    @SerializedName("code") val code: Int,

    @SerializedName("title") val title: String,

    @SerializedName("textContent") val textContent: String,

    @SerializedName("date") val date: String,

    @SerializedName("CreatedAt") val createdAt: String? = null,

    @SerializedName("UpdatedAt") val updatedAt: String? = null
)
