package com.example.jinotas.api

import android.content.Context
import android.util.Log
import com.example.jinotas.api.notesApi.DeleteNoteRequest
import com.example.jinotas.api.userApi.ApiUser
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.db.Notes
import com.example.jinotas.db.UserToken
import com.example.jinotas.utils.UtilsDBAPI.API_TOKEN
import com.example.jinotas.utils.UtilsDBAPI.URL_API_NOTES
import com.example.jinotas.utils.UtilsDBAPI.URL_API_TOKEN_USER
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CrudApi {
    private fun getClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder().header("xc-token", API_TOKEN)
            val request = requestBuilder.build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder().addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor).build()
    }

    private fun getRetrofitNotes(): Retrofit {
        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder().baseUrl(URL_API_NOTES).client(getClient())
            .addConverterFactory(GsonConverterFactory.create(gson)).build()
    }

    private fun getRetrofitUser(): Retrofit {
        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder().baseUrl(URL_API_TOKEN_USER).client(getClient())
            .addConverterFactory(GsonConverterFactory.create(gson)).build()
    }

    suspend fun getNotesList(): Notes? = withContext(Dispatchers.IO) {
        val response = getRetrofitNotes().create(ApiService::class.java).getNotesList()
        val notes = Notes()
        response!!.body()!!.list.forEach { apiNote ->
            notes.add(
                Note(
                    code = apiNote.code,
                    id = apiNote.id,
                    title = apiNote.title,
                    textContent = apiNote.textContent,
                    date = apiNote.date,
                    userFrom = apiNote.userFrom,
                    userTo = apiNote.userTo
                )
            )
        }

        return@withContext if (response.isSuccessful) notes else null
    }

    suspend fun patchNote(noteUpdate: Note): Note? = withContext(Dispatchers.IO) {
        val response = getRetrofitNotes().create(ApiService::class.java).putNote(noteUpdate)
        return@withContext if (response.isSuccessful) response.body() else null
    }

    suspend fun postNote(notePost: Note, context: Context): Note? = withContext(Dispatchers.IO) {
        val db: AppDatabase = AppDatabase.getDatabase(context)
        val response = getRetrofitNotes().create(ApiService::class.java).postNote(notePost)
        val apiNotesList = getNotesList() as ArrayList<Note>
        if (apiNotesList.size > 0) {
            for (n in apiNotesList) {
                if (n.code == notePost.code) {
                    notePost.id = n.id
                    //var notePonerId = db.noteDAO().getNoteByCode(notePost.code)
                    db.noteDAO().updateNote(notePost)
                    val notePonerId = db.noteDAO().getNoteByCode(notePost.code)
                    Log.i("NotePonerId", notePonerId.id.toString())
                    Log.i("NotaAPIId", n.id.toString())
                    Log.i("NotaDBId", notePost.id.toString())
                }
            }
        }
        return@withContext if (response.isSuccessful) response.body() else null
    }

    suspend fun deleteNote(id: Int) = withContext(Dispatchers.IO) {
        val delete = DeleteNoteRequest(Id = id)
        getRetrofitNotes().create(ApiService::class.java).deleteNote(delete)
        Log.e("codeNote", id.toString())
    }

    suspend fun getTokenByUser(user: String): UserToken? = withContext(Dispatchers.IO) {
        val response = getRetrofitUser().create(ApiService::class.java).getTokenByUser()
        val allUsers = response!!.body()!!.list
        var token = ""
        var password = ""

        allUsers.forEach { it ->
            if (it.userName == user) {
                token = it.token
                password = it.password
            }
        }

        val userToken = UserToken(userName = user, token = token, password = password)
        return@withContext if (response.isSuccessful) userToken else null
    }

    suspend fun postTokenByUser(tokenUser: UserToken): UserToken? = withContext(Dispatchers.IO) {
        val apiUser = ApiUser(
            userName = tokenUser.userName, password = tokenUser.password, token = tokenUser.token
        )
        val response = getRetrofitUser().create(ApiService::class.java).postUserToken(apiUser)
        val result = response!!.body()!!
        val user =
            UserToken(userName = result.userName, password = result.password, token = result.token)
        return@withContext if (response.isSuccessful) user else null
    }

    suspend fun patchUserToken(userTokenUpdate: UserToken): UserToken? =
        withContext(Dispatchers.IO) {
            val response = getRetrofitUser().create(ApiService::class.java).putUserToken(
                ApiUser(
                    userName = userTokenUpdate.userName,
                    password = userTokenUpdate.password,
                    token = userTokenUpdate.token
                )
            )
            val result = response!!.body()!!
            val userToken = UserToken(
                userName = result.userName, password = result.password, token = result.token
            )
            return@withContext if (response.isSuccessful) userToken else null
        }
}
