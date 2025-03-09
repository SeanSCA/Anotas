package com.example.jinotas.api

import com.example.jinotas.api.notesApi.ApiNotesResponse
import com.example.jinotas.api.notesApi.DeleteNoteRequest
import com.example.jinotas.api.userApi.ApiUser
import com.example.jinotas.api.userApi.ApiUserResponse
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
import retrofit2.http.Query


interface ApiService {
    @GET("?limit=25&shuffle=0&offset=0")
    suspend fun getNotesList(): Response<ApiNotesResponse>

    @POST("?")
    suspend fun postNote(@Body note: Note): Response<Note>

    @PATCH("?/")
    suspend fun putNote(@Body note: Note): Response<Note>

    @HTTP(method = "DELETE", path = "?", hasBody = true)
    suspend fun deleteNote(@Body request: DeleteNoteRequest): Response<Unit>

    @GET("?")
    suspend fun getTokenByUser(
        @Query("where") where: String,
        @Query("limit") limit: Int = 1,
        @Query("shuffle") shuffle: Int = 0,
        @Query("offset") offset: Int = 0
    ): Response<ApiUserResponse>


    @POST("?")
    suspend fun postUserToken(@Body tokenUser: ApiUser): Response<ApiUser>

    @PATCH("?/")
    suspend fun putUserToken(@Body userToken: ApiUser): Response<ApiUser>
}