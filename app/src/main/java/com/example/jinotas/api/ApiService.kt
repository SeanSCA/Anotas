package com.example.jinotas.api

import com.example.jinotas.db.Note
import com.example.jinotas.db.Notes
import com.example.jinotas.db.UserToken
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path


interface ApiService {
    @GET("GetAllNotes")
    suspend fun getNotesList(): Response<Notes>

    @POST("InsertNote/")
    suspend fun postNote(@Body note: Note): Response<Note>

    @PUT("UpdateNote/")
    suspend fun putNote(@Body note: Note): Response<Note>

    @DELETE("DeleteNoteById/{code}")
    suspend fun deleteNote(@Path("code") code: Int): Response<Unit>

    @GET("GetTokenByUser/{userName}")
    suspend fun getTokenByUser(@Path("userName") userName: String): Response<UserToken>

    @POST("InsertUserToken/")
    suspend fun postUserToken(@Body tokenUser: UserToken): Response<UserToken>

    @PUT("UpdateUserToken/{token}/{userName}")
    suspend fun putUserToken(@Path("token") token: String, @Path("userName") userName: String): Response<UserToken>
}