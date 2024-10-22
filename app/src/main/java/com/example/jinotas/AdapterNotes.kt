package com.example.jinotas

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.api.CrudApi
import com.example.jinotas.api.tokenusernocodb.ApiTokenUser
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.utils.Utils.getAccessToken
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class AdapterNotes(
    private var list: ArrayList<Note>, override val coroutineContext: CoroutineContext
) : RecyclerView.Adapter<AdapterNotes.ViewHolder>(), CoroutineScope {

    private lateinit var db: AppDatabase
    private var canConnect: Boolean = false

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val titleText = vista.findViewById<TextView>(R.id.tv_show_note_title)
        val notesText = vista.findViewById<TextView>(R.id.tv_show_note_content_resume)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
        return ViewHolder(layout.inflate(R.layout.cardview_notes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context: Context = holder.itemView.context
        val content = list[position].textContent
        if (content.length >= 49) {
            val resultContent = content.substring(startIndex = 0, endIndex = 48) + "..."
            holder.notesText.text = resultContent
        } else {
            val resultContent = content.substring(startIndex = 0, endIndex = content.length)
            holder.notesText.text = resultContent
        }
        holder.titleText.text = list[position].title
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ShowNoteActivity::class.java)
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            val userNameFrom = sharedPreferences.getString("userName", "")
            intent.putExtra("code", list[position].code)
            intent.putExtra("userFrom", userNameFrom)
            startActivity(context, intent, null)
        }

        holder.itemView.setOnLongClickListener {
            val popupMenu = PopupMenu(context, holder.itemView)
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_eliminar -> runBlocking {
                        val corrutina = launch {
                            db = AppDatabase.getDatabase(context)
                            val note =
                                list[position].code.let { it1 -> db.noteDAO().getNoteByCode(it1) }
                            CrudApi().deleteNote(note.id!!)
                            db.noteDAO().deleteNote(note)
                            updateList(db.noteDAO().getNotesList() as ArrayList<Note>)
                        }
                        corrutina.join()
                    }

                    R.id.action_send -> if (tryConnection()) {
                        showFormDialog(context, list[position])
                    }
                }
                true
            }
            popupMenu.show()
            true
        }
    }

    fun updateList(newList: ArrayList<Note>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun Print(context: Context, text: String) {
        return Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    override fun getItemCount() = list.size

    // Método para mostrar el formulario en un AlertDialog
    private fun showFormDialog(context: Context, note: Note) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("¿A quien se lo quieres enviar? ")

        var userToSend: String

        // Crear un Layout para el formulario
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        // Crear el campo del formulario
        val nameInput = EditText(context)
        nameInput.hint = "Nombre de usuario"
        layout.addView(nameInput)

        // Configurar el layout dentro del diálogo
        builder.setView(layout)

        // Botones del diálogo
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            // Guardar el nombre de usuario en una variable
            userToSend = nameInput.text.toString()
            Log.e("userToSend", userToSend)

            runBlocking {
                val corrutina = launch {
                    sendPushNotificationToUserWithNote(userToSend, note, context)
                }
                corrutina.join()
            }

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        // Mostrar el diálogo
        builder.show()
    }

    private fun getTokenByUser(userName: String): String {
//        var userToken: List<ApiTokenUser>? = null
        var userToken: ApiTokenUser? = null
        runBlocking {
            val corrutina = launch {
                userToken = CrudApi().getTokenByUser(userName)!!
            }
            corrutina.join()
        }
        return userToken!!.token
    }

    private fun sendPushNotificationToUserWithNote(userName: String, note: Note, context: Context) {
        var accessToken: String = ""
        val tokenReceptor = getTokenByUser(userName)
        Log.i("userFrom", note.userFrom)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                accessToken = getAccessToken(context)

                // Definir la URL para la API v1 de FCM
                val url = "https://fcm.googleapis.com/v1/projects/notemanager-15064/messages:send"

                // Convertir el objeto Note a JSON usando Gson
                val noteJson = Gson().toJson(note)

                // Crear el OkHttpClient
                val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()

                // Crear la carga JSON para enviar la notificación con datos
                val json = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", tokenReceptor) // Enviar a un token específico
                        put("notification", JSONObject().apply {
                            put("title", "Nueva Nota")
                            put("body", "${note.userFrom} te ha enviado una nueva nota")
                        })
                        // Agregar los datos personalizados, asegurándose de convertir los valores numéricos a cadenas
                        put("data", JSONObject().apply {
                            put("code", note.code.toString())  // Convertir a String
                            put(
                                "id", note.id?.toString() ?: ""
                            )  // Convertir a String o manejar null
                            put("title", note.title)
                            put("textContent", note.textContent)
                            put("date", note.date)
                            put("userFrom", note.userFrom)
                            put("userTo", note.userTo ?: "")  // Manejar null
                            put("createdAt", note.createdAt ?: "")
                            put("updatedAt", note.updatedAt ?: "")
                        })
                    })
                }

                // Crear el cuerpo de la solicitud
                val body = RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(), json.toString()
                )

                // Construir la solicitud con el encabezado de autorización
                val request = Request.Builder().url(url).post(body).addHeader(
                    "Authorization", "Bearer $accessToken"
                ).build()

                // Ejecutar la solicitud de manera asíncrona
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        println("Response: ${response.body?.string()}")
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace() // Manejar excepciones
            }
        }
    }


    /**
     * Here checks if there's connection to the api
     * @return Boolean if there's connection or not
     */
    private fun tryConnection(): Boolean {
        try {
            canConnect = CrudApi().canConnectToApi()
        } catch (e: Exception) {
            Log.e("cantConnectToApi", "No tienes conexión con la API")
        }
        return canConnect
    }
}