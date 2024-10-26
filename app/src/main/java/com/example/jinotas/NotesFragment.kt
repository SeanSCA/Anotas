package com.example.jinotas

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.adapter.AdapterNotes
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.FragmentNotesBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.google.android.material.snackbar.Snackbar
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

        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val note: Note = notesList[position]

                if (direction == ItemTouchHelper.LEFT) {
                    // Eliminar de la lista y actualizar el RecyclerView
                    notesList.removeAt(position)
                    adapterNotes.notifyItemRemoved(position)

                    // Mostrar Snackbar con opción de "Deshacer"
                    val snackbar = Snackbar.make(
                        binding.rvNotes,
                        "Has eliminado la nota ${note.title}",
                        Snackbar.LENGTH_LONG
                    )

                    snackbar.setAction("Deshacer") {
                        // Agregar nuevamente la nota en la posición original
                        notesList.add(position, note)
                        adapterNotes.notifyItemInserted(position)
                    }

                    snackbar.addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            // Si el Snackbar se cierra sin haber hecho "Deshacer", eliminamos la nota de la base de datos
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                adapterNotes.deleteNoteDBApi(
                                    this@NotesFragment.requireContext(), note
                                )
                            }
                        }
                    })

                    snackbar.show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    Log.e("tamañoListaAntes", notesList.size.toString())
                    notesList.removeAt(position)
                    adapterNotes.notifyItemRemoved(position)
                    notesList.add(position, note)
                    adapterNotes.notifyItemInserted(position)
//                    Toast.makeText(
//                        this@NotesFragment.requireContext(),
//                        "Has deslizado a la derecha",
//                        Toast.LENGTH_LONG
//                    ).show()
                        adapterNotes.sendNote(this@NotesFragment.requireContext(), note)
                    Log.e("tamañoListaDespues", notesList.size.toString())
                }

            }
        }).attachToRecyclerView(binding.rvNotes)


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