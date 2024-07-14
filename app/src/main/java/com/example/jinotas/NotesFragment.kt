package com.example.jinotas

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jinotas.databinding.FragmentNotesBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.ArrayList
import kotlin.coroutines.CoroutineContext

class NotesFragment : Fragment(), CoroutineScope {
    private lateinit var binding: FragmentNotesBinding
    private lateinit var adapterNotes: AdapterNotes
    private lateinit var notesList: ArrayList<Note>
    private lateinit var db: AppDatabase
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotesBinding.inflate(inflater)
        loadNotes()


        return binding.root
    }

    fun loadNotes() {
        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(requireContext())
                val notaExample = Note(null, "EL TITULO", "blablablablablablablablabla", "today")
                notesList = db.noteDAO().getNotes() as ArrayList<Note>
            }
            corrutina.join()
            binding.rvNotes.layoutManager = LinearLayoutManager(context)
            adapterNotes = AdapterNotes(notesList, coroutineContext)
            adapterNotes.updateList(notesList)
            binding.rvNotes.adapter = adapterNotes
        }
//        Toast.makeText(
//            requireContext(), "Has cargado las notas", Toast.LENGTH_LONG
//        ).show()
    }

    fun loadFilteredNotes(filter: String) {
        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(requireContext())
                val notaExample = Note(null, "EL TITULO", "blablablablablablablablabla", "today")
                notesList = db.noteDAO().getNoteByTitle(filter) as ArrayList<Note>
            }
            corrutina.join()
            binding.rvNotes.layoutManager = LinearLayoutManager(context)
            adapterNotes = AdapterNotes(notesList, coroutineContext)
            adapterNotes.updateList(notesList)
            binding.rvNotes.adapter = adapterNotes
        }
//        Toast.makeText(
//            requireContext(), "Has cargado las notas", Toast.LENGTH_LONG
//        ).show()
    }

    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }
        })
    }
}