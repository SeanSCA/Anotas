package com.example.jinotas.api.notesApi

import com.google.gson.annotations.SerializedName

data class DeleteNoteRequest(
    @SerializedName("Id") val Id: Int
)