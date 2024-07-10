package com.example.jinotas

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.jinotas.databinding.FragmentNotesBinding

class Notes : Fragment() {
    private lateinit var binding: FragmentNotesBinding
//    private var adapterNotes : AdapterNotes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotesBinding.inflate(inflater)
        binding.rvNotes.layoutManager = GridLayoutManager(context, 3)
//        adapterNotes = AdapterNotes(imagen_list_actual, coroutineContext)
//        binding.rvNotes.adapter =
//            adapterNotes

        return binding.root
    }
}