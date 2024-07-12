package com.example.jinotas

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        //Esto es para mostrar la lista con las notas, modificar esto para que al pulsar en un boton ponga un layout u otro, probar tambien con el adaptador
        return binding.root
    }

    fun loadNotes() {
        runBlocking {
            val corrutina = launch {
                db = AppDatabase.getDatabase(requireContext())
                val notaExample = Note(null, "EL TITULO", "blablablablablablablablabla", "today")
//                db.noteDAO().insertNote(notaExample)
                notesList = db.noteDAO().getNotes() as ArrayList<Note>
            }
            corrutina.join()
            binding.rvNotes.layoutManager = LinearLayoutManager(context)
            adapterNotes = AdapterNotes(notesList)
            adapterNotes.updateList(notesList)
            binding.rvNotes.adapter = adapterNotes
        }
        Toast.makeText(
            requireContext(), "Has cargado las notas", Toast.LENGTH_LONG
        ).show()
    }
}