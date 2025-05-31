package com.example.jinotas.adapter

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.R
import com.example.jinotas.ShowNoteActivity
import com.example.jinotas.db.Note
import com.example.jinotas.utils.ChecklistUtils
import com.example.jinotas.utils.ThemeUtils
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

class AdapterNotes(
    private var list: ArrayList<Note>, override val coroutineContext: CoroutineContext
) : RecyclerView.Adapter<AdapterNotes.ViewHolder>(), CoroutineScope {

    private val db = Firebase.firestore
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
//        whereEqualTo("code", note.code)
        var docId = ""
        db.collection("notas").whereEqualTo("code", note.code).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    for (document in result) {
                        docId = document.id

                        // Aquí se elimina la nota
                        db.collection("notas").document(docId).delete().addOnSuccessListener {
                                Log.d(
                                    "Nota eliminada", "DocumentSnapshot successfully deleted!"
                                )
                            }.addOnFailureListener { e ->
                                Log.w(
                                    "Nota no eliminada", "Error deleting document", e
                                )
                            }
                    }
                } else {
                    Log.d("Firestore", "No se encontró ninguna nota con code = $note.code")
                }
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error al buscar nota", e)
            }

    }

    fun deleteNoteDB(context: Context, note: Note) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                db = AppDatabase.getDatabase(context)
//                note.syncStatus = SyncStatus.DELETED
//                note.updatedTime = System.currentTimeMillis()
//                db.noteDAO().updateNote(note)
//
//                withContext(Dispatchers.Main) {
//                    updateList(db.noteDAO().getNotesList() as ArrayList<Note>)
//                }
//            } catch (e: Exception) {
//                Log.e("deleteNoteDB", "Error eliminando localmente: ${e.message}")
//            }
//        }
    }

    fun sendNote(context: Context, note: Note) {
//        showNestedAlertDialog(context, note = note)
    }

    private fun Print(context: Context, text: String) {
        return Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    override fun getItemCount() = list.size

//    private fun showConfirmationDialog(context: Context, note: Note) {
//        val builder = AlertDialog.Builder(context)
//        builder.setTitle(context.getString(R.string.sendNoteConfirmation))
//            .setMessage(context.getString(R.string.sendNoteConfirmationCancel))
//            .setPositiveButton("Ok") { _, _ ->
//                Toast.makeText(
//                    context,
//                    context.getString(R.string.sendNoteConfirmationCanceled),
//                    Toast.LENGTH_SHORT
//                ).show()
//            }.setNegativeButton("No") { _, _ ->
////                showNestedAlertDialog(context, note)
//            }.show()
//    }

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
                    context, androidx.appcompat.R.attr.colorAccent
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