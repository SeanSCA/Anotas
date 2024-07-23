package com.example.jinotas.api

import android.util.Log
import com.example.jinotas.db.Note
import com.example.jinotas.db.Notes
import com.google.gson.GsonBuilder
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


    private val URL_API = "https://nexttanleaf32.conveyor.cloud/"

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
                    val response = getRetrofit().create(ApiService::class.java).GetNotesList()
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
                response = getRetrofit().create(ApiService::class.java).GetNotesList()
            }
            corrutina.join()
        }

        if (response!!.isSuccessful) {
            return response!!.body()!!
        } else {
            return null
        }
    }

    fun postNote(note: Note): Note? {
        var response: Response<Note>? = null
        runBlocking {
            val corrutina = launch {
                response = getRetrofit().create(ApiService::class.java).PostNote(note)
            }
            corrutina.join()
        }
        if (response!!.isSuccessful) {
            return response!!.body()!!
        } else {
            return null
        }
    }

    fun putNote(note: Note): Note? {
        var response: Response<Note>? = null
        runBlocking {
            val corrutina = launch {
                response = getRetrofit().create(ApiService::class.java).PutNote(note)
            }
            corrutina.join()
        }
        if (response!!.isSuccessful) {
            return response!!.body()!!
        } else {
            return null
        }
    }

    fun deleteNote(id: Int) {
        runBlocking {
            val corrutina = launch {
                getRetrofit().create(ApiService::class.java).DeleteNote(id)
            }
            corrutina.join()
        }

    }
}