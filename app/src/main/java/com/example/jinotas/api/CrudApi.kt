package com.example.jinotas.api

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.example.jinotas.MainActivity
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.db.Notes
import com.example.jinotas.db.Token
import com.example.jinotas.db.UserToken
import com.google.gson.GsonBuilder
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.coroutines.CoroutineContext

class CrudApi() : CoroutineScope {
    private val job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }
    private val URL_API = dotenv["URL_API_NOTES"]

    private fun getClient(): OkHttpClient {
        var login = HttpLoggingInterceptor()
        login.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder().addInterceptor(login).build()
    }

    private fun getRetrofit(): Retrofit {
        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder().baseUrl(URL_API).client(getClient())
            .addConverterFactory(GsonConverterFactory.create(gson)).build()
    }

    fun canConnectToApi(): Boolean {
        var connected = false

        runBlocking {
            val corrutina = launch {
                try {
                    val response = getRetrofit().create(ApiService::class.java).getNotesList()
                    if (response.isSuccessful) {
                        connected = true
                    }
                } catch (e: Exception) {
                    Log.e("ConexionAPI", e.message.toString())
                }
            }
            corrutina.join()
        }

        return connected
    }

    fun getNotesList(): Notes? {
        var response: Response<Notes>? = null

        runBlocking {
            val corrutina = launch {
                response = getRetrofit().create(ApiService::class.java).getNotesList()
            }
            corrutina.join()
        }

        if (response!!.isSuccessful) {
            return response!!.body()!!
        } else {
            return null
        }
    }

    //Esto es para una unica nota
    fun patchNote(noteUpdate: Note): Note? {
        var response: Response<Note>? = null
        runBlocking {
            val corrutina = launch {
                Log.e("id", noteUpdate.id.toString())
                Log.e("code", noteUpdate.code.toString())
                Log.e("title", noteUpdate.title)
                Log.e("textContent", noteUpdate.textContent)
                response = getRetrofit().create(ApiService::class.java).putNote(noteUpdate)
            }
            corrutina.join()
        }
        return if (response!!.isSuccessful) {
            response!!.body()!!
        } else {
            null
        }
    }

    fun postNote(notePost: Note, mainContext: Context): Note? {
        var response: Response<Note>? = null
        //Todas las notas de la db
        runBlocking {
            val corrutina = launch {
                response = getRetrofit().create(ApiService::class.java).postNote(notePost)
            }
            corrutina.join()
        }
        return if (response!!.isSuccessful) {
            response!!.body()!!
        } else {
            null
        }
    }

    fun deleteNote(id: Int) {
        runBlocking {
            val corrutina = launch {
                getRetrofit().create(ApiService::class.java).deleteNote(id)
                Log.e("idNote", id.toString())
            }
            corrutina.join()
        }
    }

    fun getTokenByUser(user: String): UserToken? {
        var response: Response<UserToken>? = null
        runBlocking {
            val corrutina = launch {
                response = getRetrofit().create(ApiService::class.java).getTokenByUser(user)
            }
            corrutina.join()
        }
        if (response!!.isSuccessful) {
            return response!!.body()
        } else {
            return null
        }
    }

    fun postTokenByUser(tokenUser: UserToken): UserToken? {
        var response: Response<UserToken>? = null
        runBlocking {
            val corrutina = launch {
                response = getRetrofit().create(ApiService::class.java).postUserToken(tokenUser)
            }
            corrutina.join()
        }
        return if (response!!.isSuccessful) {
            response!!.body()!!
        } else {
            null
        }
    }

    fun patchUserToken(userTokenUpdate: UserToken): UserToken? {
        var response: Response<UserToken>? = null
        val sharedPreferences: SharedPreferences =
            MainActivity().getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userFrom", "")

        runBlocking {
            val corrutina = launch {
                Log.e("id", userTokenUpdate.token)
                Log.e("code", userName!!)
                response = getRetrofit().create(ApiService::class.java)
                    .putUserToken(userTokenUpdate.token, userName!!)
            }
            corrutina.join()
        }
        return if (response!!.isSuccessful) {
            response!!.body()!!
        } else {
            null
        }
    }

}