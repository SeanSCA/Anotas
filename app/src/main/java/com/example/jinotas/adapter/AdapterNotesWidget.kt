package com.example.jinotas.adapter

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
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.allViews
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.R
import com.example.jinotas.ShowNoteActivity
import com.example.jinotas.api.CrudApi
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.utils.ChecklistUtils
import com.example.jinotas.utils.ThemeUtils
import com.example.jinotas.utils.Utils.getAccessToken
import com.example.jinotas.widget.WidgetProvider
import com.google.gson.Gson
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
