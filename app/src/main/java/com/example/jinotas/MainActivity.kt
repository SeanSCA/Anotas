package com.example.jinotas

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        supportFragmentManager.commit {
            replace<Notes>(R.id.fragment_container_view)
            setReorderingAllowed(true)
            addToBackStack(null)
        }
        val fragment: Notes =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as Notes
    }
}