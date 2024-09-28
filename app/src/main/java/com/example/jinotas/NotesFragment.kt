package com.example.jinotas

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.FragmentNotesBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    /**
     * Load all the notes into the recyclerview
     */
    fun loadNotes() {
        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(requireContext())
                notesList = db.noteDAO().getNotesList() as ArrayList<Note>
            }
            corrutina.join()
            binding.rvNotes.layoutManager = LinearLayoutManager(context)
            adapterNotes = AdapterNotes(notesList, coroutineContext)
            adapterNotes.updateList(notesList)
            binding.rvNotes.adapter = adapterNotes
        }
    }

    fun loadNotesFromApi() {
        val notes = CrudApi().getNotesList() as ArrayList<Note>
        binding.rvNotes.layoutManager = LinearLayoutManager(context)
        adapterNotes = AdapterNotes(notes, coroutineContext)
        adapterNotes.updateList(notes)
        binding.rvNotes.adapter = adapterNotes
    }

    /**
     * Shows only the notes with the @param filter as title
     * @param filter parameter to search as a title
     */
    fun loadFilteredNotes(filter: String) {
        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(requireContext())
                notesList = db.noteDAO().getNoteByTitle(filter) as ArrayList<Note>
            }
            corrutina.join()
            binding.rvNotes.layoutManager = LinearLayoutManager(context)
            adapterNotes = AdapterNotes(notesList, coroutineContext)
            adapterNotes.updateList(notesList)
            binding.rvNotes.adapter = adapterNotes
        }
    }

    /**
     * Order the notes by title or date
     * @param type Check if the type is "title" or "date"
     */
    fun orderByNotes(type: String) {
        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(requireContext())
                if (type == "date") {
                    notesList = db.noteDAO().getNoteOrderByDate() as ArrayList<Note>
                } else if (type == "title") {
                    notesList = db.noteDAO().getNoteOrderByTitle() as ArrayList<Note>
                } else {
                    notesList = db.noteDAO().getNotesList() as ArrayList<Note>
                }
            }
            corrutina.join()
            binding.rvNotes.layoutManager = LinearLayoutManager(context)
            adapterNotes = AdapterNotes(notesList, coroutineContext)
            adapterNotes.updateList(notesList)
            binding.rvNotes.adapter = adapterNotes
        }
    }

    /**
     * Prepares the listener of the edittext
     */
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