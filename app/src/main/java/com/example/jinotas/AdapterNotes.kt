package com.example.jinotas

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.ArrayList
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
//        val notesContent = view?.findViewById<TextView>(R.id.tv_show_note_content_resume)?.text.toString()
//        notesContent.substring( )
        val content = list[position].textContent
        if(content.length >= 49){
            val resultContent = content.substring(startIndex = 0, endIndex = 48) + "..."
            holder.notesText.text = resultContent
        }else{
            val resultContent = content.substring(startIndex = 0, endIndex = content.length) + "..."
            holder.notesText.text = resultContent
        }
//        holder.notesText.text = list[position].textContent
        holder.titleText.text = list[position].title
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ShowNoteActivity::class.java)
            intent.putExtra("id", list[position].id)
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
                                list[position].id?.let { it1 -> db.noteDAO().getNoteById(it1) }
                            if (note != null) {
                                db.noteDAO().deleteNote(note)
                                updateList(db.noteDAO().getNotes() as ArrayList<Note>)
                            }
                        }
                        corrutina.join()
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

    override fun getItemCount() = list.size
}