package com.example.jinotas.api

import android.content.Context
import android.util.Log
import com.example.jinotas.db.Note
import com.example.jinotas.db.Notes
import com.example.jinotas.db.UserToken
import com.google.gson.GsonBuilder
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CrudApi {
    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }
    private val URL_API = dotenv["URL_API_NOTES"]

    private fun getClient(): OkHttpClient {
        val login = HttpLoggingInterceptor()
        login.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder().addInterceptor(login).build()
    }

    private fun getRetrofit(): Retrofit {
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder().baseUrl(URL_API).client(getClient())
            .addConverterFactory(GsonConverterFactory.create(gson)).build()
    }

    suspend fun canConnectToApi(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = getRetrofit().create(ApiService::class.java).getNotesList()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ConexionAPI", e.message.toString())
            false
        }
    }

    suspend fun getNotesList(): Notes? = withContext(Dispatchers.IO) {
        val response = getRetrofit().create(ApiService::class.java).getNotesList()
        return@withContext if (response.isSuccessful) response.body() else null
    }

    suspend fun patchNote(noteUpdate: Note): Note? = withContext(Dispatchers.IO) {
        val response = getRetrofit().create(ApiService::class.java).putNote(noteUpdate)
        return@withContext if (response.isSuccessful) response.body() else null
    }

    suspend fun postNote(notePost: Note): Note? = withContext(Dispatchers.IO) {
        val response = getRetrofit().create(ApiService::class.java).postNote(notePost)
        return@withContext if (response.isSuccessful) response.body() else null
    }

    suspend fun deleteNote(id: Int) = withContext(Dispatchers.IO) {
        getRetrofit().create(ApiService::class.java).deleteNote(id)
        Log.e("codeNote", id.toString())
    }

    suspend fun getTokenByUser(user: String): UserToken? = withContext(Dispatchers.IO) {
        val response = getRetrofit().create(ApiService::class.java).getTokenByUser(user)
        return@withContext if (response.isSuccessful) response.body() else null
    }

    suspend fun postTokenByUser(tokenUser: UserToken): UserToken? = withContext(Dispatchers.IO) {
        val response = getRetrofit().create(ApiService::class.java).postUserToken(tokenUser)
        return@withContext if (response.isSuccessful) response.body() else null
    }

    suspend fun patchUserToken(userTokenUpdate: UserToken): UserToken? = withContext(Dispatchers.IO) {
        val response = getRetrofit().create(ApiService::class.java)
            .putUserToken(userTokenUpdate.token, userTokenUpdate.userName)
        return@withContext if (response.isSuccessful) response.body() else null
    }
}
