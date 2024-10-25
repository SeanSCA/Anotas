package com.example.jinotas

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.jinotas.adapter.AdapterNotes
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.ActivityWriteNotesBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext


class WriteNotesActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityWriteNotesBinding
    private lateinit var notesList: ArrayList<Note>
    private lateinit var adapterNotes: AdapterNotes
    private lateinit var db: AppDatabase
    private var job: Job = Job()
    private var canConnect: Boolean = false

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userNameFrom = intent.getStringExtra("userFrom")

        binding.btReturnToNotes.setOnClickListener {
            finish()
        }

        binding.btSaveNote.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val current = LocalDateTime.now().format(formatter)
            runBlocking {
                val corrutina = launch {
                    val note = Note(
                        id = null,
                        title = binding.etTitle.text.toString(),
                        textContent = binding.etNoteContent.text.toString(),
                        date= current.toString(),
                        userFrom = userNameFrom!!,
                        userTo = null
                    )
                    db = AppDatabase.getDatabase(this@WriteNotesActivity)
                    db.noteDAO().insertNote(note)
                    notesList = db.noteDAO().getNotesList() as ArrayList<Note>
                    adapterNotes = AdapterNotes(notesList, coroutineContext)
                    adapterNotes.updateList(notesList)
                    uploadNoteApi(note)
                }
                corrutina.join()
            }
//            sendPushNotificationToTopic("Tienes una nota nueva")
            finish()
        }
    }

    private fun uploadNoteApi(notePost: Note) {
        if (tryConnection()) {
            runBlocking {
                val corrutina = launch {
                    CrudApi().postNote(notePost, this@WriteNotesActivity)
                    Toast.makeText(this@WriteNotesActivity, "Has subido la nota", Toast.LENGTH_LONG)
                        .show()
                }
                corrutina.join()
            }
        }
    }

//    private fun sendPushNotificationToTopic(message: String) {
//        var accessToken: String = ""
//        CoroutineScope(Dispatchers.Main).launch {
//            try {
//                accessToken = getAccessToken(this@WriteNotesActivity)
//                Log.e("accessToken", accessToken)
//
//                val deviceId = Utils.getIdDevice(context = this@WriteNotesActivity) // Obtén el ID del dispositivo
//
//                // Define la URL para la API v1 de FCM
//                val url = "https://fcm.googleapis.com/v1/projects/notemanager-15064/messages:send"
//
//                // Crear el OkHttpClient
//                val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
//
//                // Crear la carga JSON para la API v1
//                val json = JSONObject().apply {
//                    put("message", JSONObject().apply {
//                        put("topic", "global") // Enviar al tópico "global"
//                        put("notification", JSONObject().apply {
//                            put("title", "Note Manager")
//                            put("body", message)
//                        })
//                        // Agregar datos personalizados, incluyendo el deviceId
//                        put("data", JSONObject().apply {
//                            put("deviceId", deviceId) // Agregar el ID del dispositivo
//                        })
//                    })
//                }
//
//                // Crear el cuerpo de la solicitud
//                val body = RequestBody.create(
//                    "application/json; charset=utf-8".toMediaType(), json.toString()
//                )
//
//                // Construir la solicitud con el encabezado de autorización
//                val request = Request.Builder().url(url).post(body).addHeader(
//                    "Authorization", "Bearer $accessToken"
//                ).build()
//
//                // Ejecutar la solicitud de manera asíncrona
//                client.newCall(request).enqueue(object : Callback {
//                    override fun onFailure(call: Call, e: IOException) {
//                        e.printStackTrace()
//                    }
//
//                    override fun onResponse(call: Call, response: Response) {
//                        println("Response: ${response.body?.string()}")
//                    }
//                })
//            } catch (e: Exception) {
//                e.printStackTrace() // Manejar excepciones
//            }
//        }
//    }

    /**
     * Here checks if there's connection to the api
     * @return Boolean if there's connection or not
     */
    fun tryConnection(): Boolean {
        try {
            canConnect = CrudApi().canConnectToApi()
        } catch (e: Exception) {
            Log.e("cantConnectToApi", "No tienes conexión con la API")
        }
        return canConnect
    }
}