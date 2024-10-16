package com.example.jinotas

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.ActivityWriteNotesBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.concurrent.TimeUnit
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

        var userName = intent.getStringExtra("user")

        binding.btReturnToNotes.setOnClickListener {
            finish()
        }

        binding.btSaveNote.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val current = LocalDateTime.now().format(formatter)
            runBlocking {
                val corrutina = launch {
                    val note = Note(
                        null,
                        binding.etTitle.text.toString(),
                        binding.etNoteContent.text.toString(),
                        current.toString(),
                        userName!!
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
            sendPushNotificationToTopic("Tienes una nota nueva")
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

    private fun sendPushNotificationToTopic(message: String) {
        var accessToken: String = ""
        CoroutineScope(Dispatchers.Main).launch {
            try {
                accessToken = getAccessToken(this@WriteNotesActivity) // Pass the context
                // Use the token as needed
                Log.e("accessToken", accessToken)
                // Define the URL for the v1 API
                val url = "https://fcm.googleapis.com/v1/projects/notemanager-15064/messages:send"
                // Obtain the access token (implement this method to get the token)

                // Create the OkHttpClient
                val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()

                // Create the JSON payload for the v1 API
                val json = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("topic", "global") // Send to the topic "global"
                        put("notification", JSONObject().apply {
                            put("title", "Notificación Global")
                            put("body", message)
                        })
                    })
                }

                // Create the request body
                val body = RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(), json.toString()
                )

                // Build the request with the Authorization header
                val request = Request.Builder().url(url).post(body).addHeader(
                    "Authorization", "Bearer $accessToken"
                ) // Correctly format the Authorization header
                    .build()

                // Execute the request asynchronously
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        println("Response: ${response.body?.string()}")
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace() // Handle exceptions
            }
        }

    }

    suspend fun getAccessToken(context: Context): String {
        return withContext(Dispatchers.IO) {
            val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

            // Load the service account credentials from assets
            val inputStream = context.assets.open("notemanager-15064-6b4b2ba119a0.json")
            val credentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"))

            // Refresh the token if it's expired
            credentials.refreshIfExpired()

            // Return the access token
            credentials.accessToken.tokenValue
        }
    }

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