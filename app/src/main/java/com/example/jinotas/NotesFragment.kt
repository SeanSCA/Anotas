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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.adapter.AdapterNotes
import com.example.jinotas.databinding.FragmentNotesBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.utils.Utils
import com.example.jinotas.utils.Utils.vibratePhone
import com.example.jinotas.utils.UtilsInternet.isConnectionStableAndFast
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


class NotesFragment : Fragment(), CoroutineScope {
    private lateinit var binding: FragmentNotesBinding
    private lateinit var adapterNotes: AdapterNotes
    private lateinit var notesList: ArrayList<Note>
    private lateinit var db: AppDatabase
    private var job: Job = Job()
    private lateinit var notesListStyle: String
    var isRemovingNote = false

    // Lista para almacenar las notas pendientes de eliminación
    val notesToDelete = mutableListOf<Pair<Int, Note>>()

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

        lifecycleScope.launch {
            Utils.getValues(requireContext()).collect { value ->
                notesListStyle = value
                Log.d("DataStore", "Valor leído: $notesListStyle")
                loadNotes() // Recargar las notas con el estilo actualizado
            }
        }
//        Log.e("notesListStyle", notesListStyle)

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
                vibratePhone(requireContext())

                lifecycleScope.launch {
                    if (!isConnectionStableAndFast(requireContext())) {
                        if (direction == ItemTouchHelper.RIGHT) {
                            // Restaurar la nota si no hay conexión en el swipe derecho
                            adapterNotes.notifyItemChanged(position)
                            Toast.makeText(
                                requireContext(),
                                "No dispones de suficiente conexión a internet",
                                Toast.LENGTH_LONG
                            ).show()
                        } else if (direction == ItemTouchHelper.LEFT) {
                            // Realizar otra acción si no hay conexión en el swipe izquierdo
                            handleOfflineSwipeLeft(note)
                            // Eliminar de la lista y actualizar RecyclerView
                            notesList.removeAt(position)
                            adapterNotes.notifyItemRemoved(position)
                            adapterNotes.deleteNoteDB(
                                this@NotesFragment.requireContext(), note
                            )
                        }
                        return@launch
                    } else {
                        if (direction == ItemTouchHelper.LEFT) {
                            // Eliminar de la lista y actualizar RecyclerView
                            notesList.removeAt(position)
                            adapterNotes.notifyItemRemoved(position)

                            // Mostrar Snackbar con opción de "Deshacer"
                            val snackbar = Snackbar.make(
                                binding.rvNotes,
                                "Has eliminado la nota ${note.title}",
                                Snackbar.LENGTH_LONG
                            )

                            snackbar.setAction("Deshacer") {
                                // Restaurar la nota en la posición original
                                notesList.add(position, note)
                                adapterNotes.notifyItemInserted(position)
                            }

                            snackbar.addCallback(object : Snackbar.Callback() {
                                override fun onDismissed(
                                    transientBottomBar: Snackbar?, event: Int
                                ) {
                                    if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                        adapterNotes.deleteNoteDBApi(
                                            this@NotesFragment.requireContext(), note
                                        )
                                    }
                                }
                            })
                            snackbar.show()
                        } else if (direction == ItemTouchHelper.RIGHT) {
                            Log.e("enviarNota", "Calidad de internet correcta")
                            notesList.removeAt(position)
                            adapterNotes.notifyItemRemoved(position)
                            notesList.add(position, note)
                            adapterNotes.notifyItemInserted(position)
                            adapterNotes.sendNote(this@NotesFragment.requireContext(), note)
                            Log.e("tamañoListaDespues", notesList.size.toString())
                        }
                    }
                }
            }
        }).attachToRecyclerView(binding.rvNotes)




        return binding.root
    }


    fun handleOfflineSwipeLeft(note: Note) {
        Log.i("eliminarDespues", "Nota ${note.title} se eliminará al recuperar internet")


    }

    fun processDeletionQueue() {
        // Si ya hay una eliminación en curso, no hacemos nada
        if (isRemovingNote) return

        if (notesToDelete.isNotEmpty()) {
            isRemovingNote = true

            // Obtenemos la primera nota en la cola
            val (position, note) = notesToDelete.removeAt(0)

            // Mostrar el Snackbar antes de realizar la eliminación visual
            val snackbar = Snackbar.make(
                binding.rvNotes, "Has eliminado la nota ${note.title}", Snackbar.LENGTH_LONG
            )

            snackbar.setAction("Deshacer") {
                // Revertir la eliminación si se selecciona "Deshacer"
                notesList.add(position, note)
                adapterNotes.notifyItemInserted(position)
            }

            snackbar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                        // Si no se deshizo, eliminamos la nota de la base de datos
                        lifecycleScope.launch {
                            adapterNotes.deleteNoteDBApi(requireContext(), note)
                        }
                    }

                    // Eliminar de la lista y actualizar la vista
                    notesList.removeAt(position)
                    adapterNotes.notifyItemRemoved(position)

                    // Continuar procesando la cola
                    isRemovingNote = false
                    processDeletionQueue()
                }
            })

            snackbar.show()
        }
    }

    /**
     * Load all the notes into the recyclerview
     */
    fun loadNotes() {
        if (::notesListStyle.isInitialized) {
            lifecycleScope.launch {
                // Leer las notas de la base de datos de manera asíncrona
                db = AppDatabase.getDatabase(requireContext())
                notesList = db.noteDAO().getNotesList() as ArrayList<Note>
                adapterNotes = AdapterNotes(notesList, coroutineContext)
                Log.i("cargarNotas", "ha cargado las notas")
                showNotes()
            }
        }
    }

    /**
     * Shows only the notes with the @param filter as title
     * @param filter parameter to search as a title
     */
    fun loadFilteredNotes(filter: String) {
        lifecycleScope.launch {
            db = AppDatabase.getDatabase(requireContext())
            notesList = db.noteDAO().getNoteByTitle(filter) as ArrayList<Note>
            adapterNotes = AdapterNotes(notesList, coroutineContext)
        }
        showNotes()

    }

    /**
     * Order the notes by title or date
     * @param type Check if the type is "title" or "date"
     */
    fun orderByNotes(type: String) {
        lifecycleScope.launch {
            db = AppDatabase.getDatabase(requireContext())
            notesList = when (type) {
                "date" -> {
                    db.noteDAO().getNoteOrderByDate() as ArrayList<Note>
                }

                "title" -> {
                    db.noteDAO().getNoteOrderByTitle() as ArrayList<Note>
                }

                else -> {
                    db.noteDAO().getNotesList() as ArrayList<Note>
                }
            }
            adapterNotes = AdapterNotes(notesList, coroutineContext)

        }
        showNotes()

    }

    private fun showNotes() {
        // Configurar el RecyclerView según el estilo de la lista (vertical o widget)
        when (notesListStyle) {
            "Vertical" -> {
                binding.rvNotes.layoutManager = LinearLayoutManager(context)
                adapterNotes.updateList(notesList)
                binding.rvNotes.adapter = adapterNotes
            }

            "Widget" -> {
                binding.rvNotes.layoutManager = GridLayoutManager(context, 2)
                adapterNotes.updateList(notesList)
                binding.rvNotes.adapter = adapterNotes
            }

            else -> {
                Log.e("errorVerticalWidget", "No se ha seleccionado ningún estilo de lista")
            }
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