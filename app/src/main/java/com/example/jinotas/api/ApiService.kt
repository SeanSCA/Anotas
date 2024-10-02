package com.example.jinotas.api

import com.example.jinotas.api.notesnocodb.ApiResponse
import com.example.jinotas.db.Note
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("?limit=25&shuffle=0&offset=0")
    suspend fun getNotesList(): Response<ApiResponse>

    @GET("?{id}")
    suspend fun getNoteById(@Path("id") id: Int): Response<Note>

    @POST("?")
    suspend fun postNote(@Body note: Note): Response<Note>

    @PUT("/api/Put")
    suspend fun putNote(@Body note: Note): Response<Note>

    @DELETE("/api/Delete/{id}")
    suspend fun deleteNote(@Path("id") id: Int): Response<Note>
}