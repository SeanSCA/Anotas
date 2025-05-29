package com.example.jinotas.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.R
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

class AdapterNotesWidget(
    private var list: ArrayList<Note>,
    override val coroutineContext: CoroutineContext,
    private val onNoteSelected: (Note) -> Unit
) : RecyclerView.Adapter<AdapterNotesWidget.ViewHolder>(), CoroutineScope {

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val titleText: TextView = vista.findViewById(R.id.tv_show_note_title)
        val notesText: TextView = vista.findViewById(R.id.tv_show_note_content_resume)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
        return ViewHolder(layout.inflate(R.layout.cardview_notes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = list[position]
        holder.titleText.text = note.title
        holder.notesText.text = note.textContent

        holder.itemView.setOnClickListener {
            onNoteSelected(note)
        }
    }

    override fun getItemCount() = list.size
}
