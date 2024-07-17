package com.example.jinotas.api

import com.example.jinotas.db.Note
import com.example.jinotas.db.Notes
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("/Get")
    fun GetNotesList(): Response<Notes>
}