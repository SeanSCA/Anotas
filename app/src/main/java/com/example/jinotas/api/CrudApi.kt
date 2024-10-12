package com.example.jinotas.api

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.jinotas.api.notesnocodb.ApiResponse
import com.example.jinotas.api.notesnocodb.DeleteNoteRequest
import com.example.jinotas.db.AppDatabase
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
        filename = "env"
    }
    private val URL_API = dotenv["URL_API"]
    private val API_TOKEN = dotenv["API_TOKEN"]

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
                                apiNote.code,
                                apiNote.id,
                                apiNote.title,
                                apiNote.textContent,
                                apiNote.date,
                                apiNote.user
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
                    apiNote.code, apiNote.id, apiNote.title, apiNote.textContent, apiNote.date, apiNote.user
                )
            })
            notes
        }
    }

    //Esto es para modificar todas las notas
    private fun getIdFromNotes(mainContext: Context): Notes? {
        var db: AppDatabase
        db = AppDatabase.getDatabase(mainContext)
        //Todas las notas de la api
        val apiNotesList = getNotesList() as ArrayList<Note>
        //Todas las notas de la db
        val dbNotesList = db.noteDAO().getNotesList() as ArrayList<Note>
        var noteSearch = Notes()
        runBlocking {
            val corrutina = launch {
                if (apiNotesList.size > 0) {
                    for (n in apiNotesList) {
                        noteSearch = dbNotesList.filter { it.code == n.code } as Notes
                        if (noteSearch.size > 0) {
                            dbNotesList.forEach { note ->
                                note.id = n.id
                                Log.i("NotaAPIId", n.id.toString())
                                Log.i("NotaDBId", note.id.toString())
                            }
                        } else {
                            Toast.makeText(
                                mainContext,
                                "No tienes estas notas en la nube, súbelas antes",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        mainContext,
                        "No tienes ninguna nota en la nube, súbelas antes",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            corrutina.join()
        }
        return noteSearch
    }

    //Esto es para todas las notas
    fun putNotes(mainContext: Context): Note? {
        var response: Response<Note>? = null
        val allNotesToId = getIdFromNotes(mainContext)
        runBlocking {
            val corrutina = launch {
                allNotesToId?.forEach { note ->
                    response = getRetrofit().create(ApiService::class.java).putNote(note)
                }
            }
            corrutina.join()
        }
        if (response!!.isSuccessful) {
            return response!!.body()!!
        } else {
            return null
        }
    }

    //Esto es para modificar una unica nota
    private fun getIdFromNote(mainContext: Context, noteUpdate: Note): Note {
        val db: AppDatabase = AppDatabase.getDatabase(mainContext)
        //Todas las notas de la api
        val apiNotesList = getNotesList() as ArrayList<Note>
        //Todas las notas de la db
        val noteSearch: Note = noteUpdate
        runBlocking {
            val corrutina = launch {
                if (apiNotesList.size > 0) {
                    for (n in apiNotesList) {
                        if (n.code == noteUpdate.code) {
                            noteSearch.id = n.id
                            Log.i("NotaAPIId", n.id.toString())
                            Log.i("NotaDBId", noteSearch.id.toString())
                        }
                    }
                } else {
                    Toast.makeText(
                        mainContext,
                        "No tienes ninguna nota en la nube, súbelas antes",
                        Toast.LENGTH_LONG
                    ).show()
                }
                db.noteDAO().updateNote(noteSearch)
            }
            corrutina.join()
        }
        return noteSearch
    }

    //Esto es para una unica nota
    fun patchNote(noteUpdate: Note): Note? {
        var response: Response<Note>? = null
        //val noteToId = getIdFromNote(mainContext, noteUpdate)
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
        val db: AppDatabase = AppDatabase.getDatabase(mainContext)
        var response: Response<Note>? = null
        //Todas las notas de la db
        runBlocking {
            val corrutina = launch {
                response = getRetrofit().create(ApiService::class.java).postNote(notePost)

                val apiNotesList = getNotesList() as ArrayList<Note>
                if (apiNotesList.size > 0) {
                    for (n in apiNotesList) {
                        if (n.code == notePost.code) {
                            notePost.id = n.id
                            //var notePonerId = db.noteDAO().getNoteByCode(notePost.code)
                            db.noteDAO().updateNote(notePost)
                            var notePonerId = db.noteDAO().getNoteByCode(notePost.code)
                            Log.i("NotePonerId", notePonerId.id.toString())
                            Log.i("NotaAPIId", n.id.toString())
                            Log.i("NotaDBId", notePost.id.toString())
                        }
                    }
                }
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
                val delete = DeleteNoteRequest(Id = id)
                getRetrofit().create(ApiService::class.java).deleteNote(delete)
                Log.e("idNote", id.toString())
            }
            corrutina.join()
        }
    }

}