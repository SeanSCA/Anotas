package com.example.jinotas

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import java.util.ArrayList

class AdapterNotes(private var list: ArrayList<Note>/*, var coroutineScope: CoroutineScope*/) : RecyclerView.Adapter<AdapterNotes.ViewHolder>() {

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val notesText = vista.findViewById<TextView>(R.id.tv_show_note)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
        return ViewHolder(layout.inflate(R.layout.cardview_notes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.notesText.text = list[position].textContext
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ShowNoteActivity::class.java)
            intent.putExtra("id", list[position].id)
//            Toast.makeText(holder.itemView.context, list[position].id.toString(), Toast.LENGTH_LONG).show()
            startActivity(holder.itemView.context, intent, null)
        }
    }

    fun updateList(newList: ArrayList<Note>){
        list = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = list.size
}