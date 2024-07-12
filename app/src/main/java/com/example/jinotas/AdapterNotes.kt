package com.example.jinotas

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import java.util.ArrayList
import kotlin.coroutines.CoroutineContext

class AdapterNotes(
    private var list: ArrayList<Note>, override val coroutineContext: CoroutineContext
) : RecyclerView.Adapter<AdapterNotes.ViewHolder>(), CoroutineScope {

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val notesText = vista.findViewById<TextView>(R.id.tv_show_note)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
        return ViewHolder(layout.inflate(R.layout.cardview_notes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.notesText.text = list[position].textContent
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
                    R.id.action_eliminar -> Toast.makeText(
                        holder.itemView.context, "You Clicked : " + item.title, Toast.LENGTH_SHORT
                    ).show()
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