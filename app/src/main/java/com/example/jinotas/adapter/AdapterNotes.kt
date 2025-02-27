package com.example.jinotas.adapter

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.R
import com.example.jinotas.ShowNoteActivity
import com.example.jinotas.api.CrudApi
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.utils.ChecklistUtils
import com.example.jinotas.utils.ThemeUtils
import com.example.jinotas.utils.Utils.getAccessToken
import com.google.gson.Gson
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class AdapterNotes(
    private var list: ArrayList<Note>, override val coroutineContext: CoroutineContext
) : RecyclerView.Adapter<AdapterNotes.ViewHolder>(), CoroutineScope {

    private lateinit var db: AppDatabase
    private var canConnect: Boolean = false
    val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val titleText: TextView = vista.findViewById<TextView>(R.id.tv_show_note_title)
        val notesText: TextView = vista.findViewById<TextView>(R.id.tv_show_note_content_resume)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
        return ViewHolder(layout.inflate(R.layout.cardview_notes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context: Context = holder.itemView.context
        val content = list[position].textContent
        holder.titleText.text = list[position].title

        if (content.length >= 49) {
            val resultContent = content.substring(startIndex = 0, endIndex = 48) + "..."
//            holder.notesText.text = resultContent
            val textoEditable: Editable = Editable.Factory.getInstance().newEditable(resultContent)
            processChecklists(textoEditable, context, holder.notesText)

        } else {
            val resultContent = content.substring(startIndex = 0, endIndex = content.length)
//            holder.notesText.text = resultContent
            val textoEditable: Editable = Editable.Factory.getInstance().newEditable(resultContent)
            processChecklists(textoEditable, context, holder.notesText)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ShowNoteActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                context, R.anim.fade_in, R.anim.fade_out
            )
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            val userNameFrom = sharedPreferences.getString("userFrom", "")
            intent.putExtra("code", list[position].code)
            intent.putExtra("userFrom", userNameFrom)
            startActivity(context, intent, options.toBundle())

        }

//        holder.itemView.setOnLongClickListener {
//            val popupMenu = PopupMenu(context, holder.itemView)
//            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
//            popupMenu.setOnMenuItemClickListener { item ->
//                when (item.itemId) {
//                    R.id.action_eliminar -> deleteNoteDBApi(context, list[position])
//                    R.id.action_send -> if (tryConnection()) {
////                        showFormDialog(context, list[position])
//                        showNestedAlertDialog(context, note = list[position])
//                    } else {
//                        Toast.makeText(context, "No tienes conexión", Toast.LENGTH_LONG).show()
//                    }
//                }
//                true
//            }
//            popupMenu.show()
//            true
//        }
    }

    fun updateList(newList: ArrayList<Note>) {
        list = newList
        notifyDataSetChanged()
    }

    fun deleteNoteDBApi(context: Context, note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            db = AppDatabase.getDatabase(context)
            var existingNote: Note? = null
            try {
                existingNote = note.code.let { db.noteDAO().getNoteByCode(it) }
                Log.e("notaeliminar", existingNote.code.toString())
                CrudApi().deleteNote(existingNote.id!!)  // Llamada a la API para borrar la nota mediante la Id

                db.noteDAO()
                    .deleteNoteWithTransaction(existingNote)  // Eliminación en la base de datos local

                // Actualización de la lista y notificación en el hilo principal
                withContext(Dispatchers.Main) {
                    updateList(db.noteDAO().getNotesList() as ArrayList<Note>)
                }

            } catch (e: Exception) {
                // Manejo de errores
                Log.e("deleteNoteDBApi", "Error eliminando la nota: ${e.message}")
            } finally {
                if (existingNote != null) {
                    db.noteDAO()
                        .deleteNoteWithTransaction(existingNote)  // Eliminación en la base de datos local
                }
            }
        }
    }

    fun deleteNoteDB(context: Context, note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            db = AppDatabase.getDatabase(context)
            var existingNote: Note? = null
            try {
                existingNote = note.code.let { db.noteDAO().getNoteByCode(it) }
                Log.e("notaeliminar", existingNote.code.toString())
                db.noteDAO()
                    .deleteNoteWithTransaction(existingNote)  // Eliminación en la base de datos local

                // Actualización de la lista y notificación en el hilo principal
                withContext(Dispatchers.Main) {
                    updateList(db.noteDAO().getNotesList() as ArrayList<Note>)
                }

            } catch (e: Exception) {
                // Manejo de errores
                Log.e("deleteNoteDBApi", "Error eliminando la nota: ${e.message}")
            }
        }
    }

    fun sendNote(context: Context, note: Note) {
        showNestedAlertDialog(context, note = note)
    }

    private fun Print(context: Context, text: String) {
        return Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    override fun getItemCount() = list.size

    private fun showNestedAlertDialog(context: Context, note: Note) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.sendNoteWho))

        // Crear el campo de entrada
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        val nameInput =
            EditText(context).apply { hint = context.getString(R.string.sendNoteUserName) }
        layout.addView(nameInput)
        builder.setView(layout)

        builder.setPositiveButton(context.getString(R.string.sendNoteAccept)) { _, _ ->
            // Llamar a la función suspensiva dentro de una corrutina
            val userToSend = nameInput.text.toString().lowercase()
            CoroutineScope(Dispatchers.IO).launch {
                val token = getTokenByUser(userToSend)
                if (token != null) {
                    sendPushNotificationToUserWithNote(userToSend, note, context)
                } else {
                    // Se asegura de que el código de UI se ejecute en el hilo principal
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.sendNoteUserNotExists), Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }.setNegativeButton(context.getString(R.string.sendNoteDecline)) { _, _ ->
            showConfirmationDialog(
                context, note
            )
        }.show()
    }


    private fun showConfirmationDialog(context: Context, note: Note) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.sendNoteConfirmation)).setMessage(context.getString(R.string.sendNoteConfirmationCancel))
            .setPositiveButton("Ok") { _, _ ->
                Toast.makeText(context, context.getString(R.string.sendNoteConfirmationCanceled), Toast.LENGTH_SHORT).show()
            }.setNegativeButton("No") { _, _ ->
                showNestedAlertDialog(context, note)
            }.show()
    }

    // Método para mostrar el formulario en un AlertDialog
//    private fun showFormDialog(context: Context, note: Note) {
//        val builder = AlertDialog.Builder(context)
//        builder.setTitle("¿A quien se lo quieres enviar? ")
//
//        var userToSend: String
//
//        // Crear un Layout para el formulario
//        val layout = LinearLayout(context)
//        layout.orientation = LinearLayout.VERTICAL
//
//        // Crear el campo del formulario
//        val nameInput = EditText(context)
//        nameInput.hint = "Nombre de usuario"
//        layout.addView(nameInput)
//
//        // Configurar el layout dentro del diálogo
//        builder.setView(layout)
//
//        // Botones del diálogo
//        builder.setPositiveButton("Aceptar") { dialog, _ ->
//            // Guardar el nombre de usuario en una variable
//            userToSend = nameInput.text.toString().lowercase()
//
//            Log.e("userToSend", userToSend)
//
//            if (getTokenByUser(userToSend) != null) {
//                runBlocking {
//                    val corrutina = launch {
//                        sendPushNotificationToUserWithNote(userToSend, note, context)
//                    }
//                    corrutina.join()
//                }
//                dialog.dismiss()
//            } else {
//                dialog.dismiss()
//                Toast.makeText(context, "El nombre de usuario no existe", Toast.LENGTH_LONG).show()
//            }
//
//
//        }
//
//        builder.setNegativeButton("Cancelar") { dialog, _ ->
//            dialog.cancel()
//        }
//
//        // Mostrar el diálogo
//        builder.show()
//    }

    private suspend fun getTokenByUser(userName: String): String? = withContext(Dispatchers.IO) {
        return@withContext CrudApi().getTokenByUser(userName)?.token
    }


    private fun sendPushNotificationToUserWithNote(userName: String, note: Note, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val accessToken = getAccessToken(context)
            val tokenReceptor = getTokenByUser(userName)

            if (tokenReceptor != null) {
                val url = dotenv["URL_SEND_MESSAGE"]

                Log.e("urlFire", url)
                val noteJson = Gson().toJson(note)
                Log.e("idNota", note.id.toString())

                val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
                val json = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", tokenReceptor)
                        put("data", JSONObject().apply {
                            put("id", note.id.toString())
                            put("code", note.code.toString())
                            put("title", note.title)
                            put("textContent", note.textContent)
                            put("date", note.date)
                            put("userFrom", note.userFrom)
                            put("userTo", userName)
                        })
                    })
                }

                updateNoteUserTo(note, userName, context)

                val body = RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(), json.toString()
                )
                val request = Request.Builder().url(url).post(body)
                    .addHeader("Authorization", "Bearer $accessToken").build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.i("sendNotification", "Response: ${response.body?.string()}")
                    }
                })
            } else {
                Log.e("sendNotification", "Token receptor es nulo")
            }
        }
    }


    private fun updateNoteUserTo(note: Note, userTo: String, context: Context) {
        note.userTo = userTo
        CoroutineScope(Dispatchers.IO).launch {
            db = AppDatabase.getDatabase(context)
            db.noteDAO().updateNote(note)
            CrudApi().patchNote(note)
            Log.i("noteUpdateUserTo", "Notas actualizadas en BD local y api")

        }
    }

    private fun processChecklists(content: Editable, context: Context, textView: TextView) {
        if (content.isEmpty()) {
            return
        }

        try {
            ChecklistUtils.addChecklistSpansForRegexAndColor(
                context,
                content,
                ChecklistUtils.CHECKLIST_REGEX_LINES_CHECKED,
                ThemeUtils().getColorResourceFromAttribute(
                    context, com.onesignal.R.attr.colorAccent
                ),
                false
            )
            ChecklistUtils.addChecklistSpansForRegexAndColor(
                context,
                content,
                ChecklistUtils.CHECKLIST_REGEX_LINES_UNCHECKED,
                ThemeUtils().getColorResourceFromAttribute(
                    context, androidx.appcompat.R.attr.colorAccent
                ),
                false
            )

            textView.text = content
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}