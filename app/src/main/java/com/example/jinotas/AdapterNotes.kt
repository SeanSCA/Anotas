package com.example.jinotas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.db.Note
import java.util.ArrayList

class AdapterNotes(private var list: ArrayList<Note>) : RecyclerView.Adapter<AdapterNotes.ViewHolder>() {

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val notesText = vista.findViewById<TextView>(R.id.tv_show_note)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
        return ViewHolder(layout.inflate(R.layout.cardview_notes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.notesText.text = list[position].textContext
    }

    fun updateList(newList: ArrayList<Note>){
        list = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = list.size
}