package com.example.jinotas.api.notesnocodb

import com.google.gson.annotations.SerializedName

data class DeleteNoteRequest(
    @SerializedName("Id") val Id: Int,
    )