package com.example.jinotas

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.jinotas.databinding.ActivityWriteNotesBinding

class WriteNotesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWriteNotesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWriteNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btReturnToNotes.setOnClickListener{
            val intent = Intent(this@WriteNotesActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }
}