package com.example.jinotas

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // this method is called
                // when the item is moved.
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // this method is called when we swipe our item to right direction.
                // on below line we are getting the item at a particular position.
                val deletedCourse: Note =
                    notesList[viewHolder.adapterPosition]

                // below line is to get the position
                // of the item at that position.
                val position = viewHolder.adapterPosition

                // this method is called when item is swiped.
                // below line is to remove item from our array list.
                notesList.removeAt(viewHolder.adapterPosition)

                // below line is to notify our item is removed from adapter.
                adapterNotes.notifyItemRemoved(viewHolder.adapterPosition)

                // below line is to display our snackbar with action.
                binding.rvNotes.layoutManager = LinearLayoutManager(context)

                Snackbar.make(binding.rvNotes, "Deleted " + deletedCourse.title, Snackbar.LENGTH_LONG)
                    .setAction(
                        "Undo",
                        View.OnClickListener {
                            // adding on click listener to our action of snack bar.
                            // below line is to add our item to array list with a position.
                            notesList.add(position, deletedCourse)

                            // below line is to notify item is
                            // added to our adapter class.
                            adapterNotes.notifyItemInserted(position)
                        }).show()
            }
            // at last we are adding this
            // to our recycler view.
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