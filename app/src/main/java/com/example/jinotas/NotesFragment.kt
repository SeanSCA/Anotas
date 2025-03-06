package com.example.jinotas

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jinotas.adapter.AdapterNotes
import com.example.jinotas.databinding.FragmentNotesBinding
import com.example.jinotas.db.Note
import com.example.jinotas.utils.Utils.vibratePhone
import com.example.jinotas.utils.UtilsInternet.isConnectionStableAndFast
import com.example.jinotas.viewmodels.MainViewModel
import com.example.jinotas.widget.WidgetProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {
    private lateinit var binding: FragmentNotesBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var adapterNotes: AdapterNotes
    private lateinit var notesList: ArrayList<Note>
    private lateinit var notesListStyle: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotesBinding.inflate(inflater)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        loadNotes()

        getListType()
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
                                        cleanWidgetOnDeleteNote(note)
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

    /**
     * Gets the type of the list
     */
    private fun getListType() {
        mainViewModel.notesListStyle.observe(viewLifecycleOwner) { value ->
            notesListStyle = value
            Log.d("DataStore", "Valor leído: $notesListStyle")
            loadNotes()
        }

        mainViewModel.loadNotesStyle()
    }

    fun handleOfflineSwipeLeft(note: Note) {
        Log.i("eliminarDespues", "Nota ${note.title} se eliminará al recuperar internet")
    }

    /**
     * Load all the notes into the recyclerview
     */
    fun loadNotes() {
        if (::notesListStyle.isInitialized) {
            lifecycleScope.launch {
                notesList = mainViewModel.loadNotes()!!
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
            notesList = mainViewModel.filterNotes(filter) as ArrayList<Note>
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
            notesList = when (type) {
                "date" -> {
                    mainViewModel.orderByNotes("date")
                }

                "title" -> {
                    mainViewModel.orderByNotes("title")
                }

                else -> {
                    mainViewModel.orderByNotes("")
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

    private fun cleanWidgetOnDeleteNote(note: Note) {
        val context = requireContext()
        val sharedPreferences = context.getSharedPreferences("WidgetPrefs", MODE_PRIVATE)
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, WidgetProvider::class.java)
        )

        for (appWidgetId in widgetIds) {
            val savedNoteCode = sharedPreferences.getString(
                "widget_note_${appWidgetId}_code", ""
            )

            // Verifica si la nota actualizada es la que está asociada al widget
            if (savedNoteCode == note.code.toString()) {
                WidgetProvider.updateWidget(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    getString(R.string.note_title_widget),
                    getString(R.string.note_textcontent_widget)
                )
                Toast.makeText(context, getString(R.string.deletedNoteWidget), Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}