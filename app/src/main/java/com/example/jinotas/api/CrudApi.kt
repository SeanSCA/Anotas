package com.example.jinotas.api

import android.util.Log
import com.example.jinotas.api.notesnocodb.ApiNote
import com.example.jinotas.api.notesnocodb.ApiResponse
import com.example.jinotas.db.Note
import com.example.jinotas.db.Notes
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
        filename = "env" // instead of '.env', use 'env'
    }
    private val URL_API = dotenv["URL_API"]
    private val API_TOKEN = dotenv["API_TOKEN"]

    private fun getClient(): OkHttpClient {
        // Interceptor para registrar las solicitudes y respuestas
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Interceptor para agregar el token de autenticación
        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("xc-token", API_TOKEN) // Usar 'xc-auth' en lugar de 'Authorization'
            val request = requestBuilder.build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor) // Añade el interceptor de autenticación
            .build()
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
        var response: Response<ApiResponse>? = null

        runBlocking {
            val corrutina = launch {
                response = getRetrofit().create(ApiService::class.java).getNotesList()
                if (response!!.isSuccessful) {
                    val notes = Notes()
                    response!!.body()!!.list.forEach { apiNote ->
                        notes.add(
                            Note(
                                apiNote.id,
                                apiNote.title,
                                apiNote.textContent,
                                apiNote.date
                            )
                        )
                    }
                    return@launch
                }
            }
            corrutina.join()
        }

        return response?.body()?.list?.let { list ->
            val notes = Notes()
            notes.addAll(list.map { apiNote ->
                Note(
                    apiNote.id,
                    apiNote.title,
                    apiNote.textContent,
                    apiNote.date
                )
            })
            notes
        }
    }

    fun postNote(note: Note): Note? {
        var response: Response<Note>? = null
        runBlocking {
            val corrutina = launch {
                response = getRetrofit().create(ApiService::class.java).postNote(note)
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
                response = getRetrofit().create(ApiService::class.java).putNote(note)
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
                getRetrofit().create(ApiService::class.java).deleteNote(id)
            }
            corrutina.join()
        }

    }
}