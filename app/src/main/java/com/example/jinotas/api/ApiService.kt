package com.example.jinotas.api

import com.example.jinotas.api.notesnocodb.ApiResponse as ApiResponseNotes
import com.example.jinotas.api.tokenusernocodb.ApiResponse as ApiResponseTokenUser
import com.example.jinotas.api.notesnocodb.DeleteNoteRequest
import com.example.jinotas.api.tokenusernocodb.ApiTokenUser
import com.example.jinotas.db.Note
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path


interface ApiService {
    @GET("?limit=25&shuffle=0&offset=0")
    suspend fun getNotesList(): Response<ApiResponseNotes>

    @GET("?{id}")
    suspend fun getNoteById(@Path("id") id: Int): Response<Note>

    @POST("?")
    suspend fun postNote(@Body note: Note): Response<Note>

    @PATCH("?/")
    suspend fun putNote(@Body note: Note): Response<Note>

    @HTTP(method = "DELETE", path = "?", hasBody = true)
    suspend fun deleteNote(@Body request: DeleteNoteRequest): Response<Unit>

    @GET("?limit=25&shuffle=0&offset=0")
    suspend fun getTokenByUser(): Response<ApiResponseTokenUser>

    @POST("?")
    suspend fun postUserToken(@Body tokenUser: ApiTokenUser): Response<ApiTokenUser>

    @PATCH("?/")
    suspend fun putUserToken(@Body userToken: ApiTokenUser): Response<ApiTokenUser>
}