package com.example.jinotas.api

import com.example.jinotas.db.Note
import com.example.jinotas.db.Notes
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/api/Get")
    suspend fun GetNotesList(): Response<Notes>

    @POST("/api/Post")
    suspend fun PostNote(@Body note: Note): Response<Note>

    @PUT("/api/Put")
    suspend fun PutNote(@Body note: Note): Response<Note>

    @DELETE("/api/Delete/{id}")
    suspend fun DeleteNote(@Path("id") id: Int) : Response<Note>
}