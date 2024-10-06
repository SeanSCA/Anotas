package com.example.jinotas

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.api.CrudApi
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

class AdapterNotes(
    private var list: ArrayList<Note>, override val coroutineContext: CoroutineContext
) : RecyclerView.Adapter<AdapterNotes.ViewHolder>(), CoroutineScope {

    private lateinit var db: AppDatabase

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val titleText = vista.findViewById<TextView>(R.id.tv_show_note_title)
        val notesText = vista.findViewById<TextView>(R.id.tv_show_note_content_resume)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
        return ViewHolder(layout.inflate(R.layout.cardview_notes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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
            val intent = Intent(holder.itemView.context, ShowNoteActivity::class.java)
            intent.putExtra("code", list[position].code)
            startActivity(holder.itemView.context, intent, null)
        }

        holder.itemView.setOnLongClickListener {
            val popupMenu = PopupMenu(holder.itemView.context, holder.itemView)
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_eliminar -> runBlocking {
                        val corrutina = launch {
                            db = AppDatabase.getDatabase(holder.itemView.context)
                            val note =
                                list[position].code?.let { it1 -> db.noteDAO().getNoteByCode(it1) }
                            if (note != null) {
                                db.noteDAO().deleteNote(note)
                                updateList(db.noteDAO().getNotesList() as ArrayList<Note>)
                            }
                        }
                        corrutina.join()
                    }

                    R.id.action_eliminar_api -> if (CrudApi().canConnectToApi()) {
                        runBlocking {
                            val corrutina = launch {
                                val delNote = "Has eliminado la nota " + list[position].title
                                //API
                                CrudApi().deleteNote(list[position].id!!)
                                //DB
                                db = AppDatabase.getDatabase(holder.itemView.context)
                                val note = list[position].code?.let { it1 ->
                                    db.noteDAO().getNoteByCode(it1)
                                }
                                if (note != null) {
                                    db.noteDAO().deleteNote(note)
                                }
                                updateList(db.noteDAO().getNotesList() as ArrayList<Note>)

                                Print(holder.itemView.context, delNote)
                            }
                            corrutina.join()
                        }
                    } else {
                        Print(holder.itemView.context, "No tienes conexión con la API")
                    }

                    R.id.action_modificar_api -> if (CrudApi().canConnectToApi()) {
                        CrudApi().putNote(holder.itemView.context, list[position])
                        Print(holder.itemView.context, "Has modificado la nota en la API")
                    } else {
                        Print(holder.itemView.context, "No tienes conexión con la API")
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
}