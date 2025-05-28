package com.example.jinotas

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.jinotas.custom_textview.CustomEditText
import com.example.jinotas.databinding.ActivityWriteNotesBinding
import com.example.jinotas.db.Note
import com.example.jinotas.utils.ChecklistUtils
import com.example.jinotas.utils.Utils.vibratePhone
import com.example.jinotas.viewmodels.MainViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class WriteNotesActivity : AppCompatActivity(), TextWatcher, OnFocusChangeListener,
    CustomEditText.OnSelectionChangedListener, CustomEditText.OnCheckboxToggledListener {
    private lateinit var binding: ActivityWriteNotesBinding
    private lateinit var mainViewModel: MainViewModel

    //Markdown
    private lateinit var mContentEditText: CustomEditText

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

//        val userNameFrom = intent.getStringExtra("userFrom")
        mContentEditText = binding.noteContent
        binding.btReturnToNotes.setOnClickListener {
            vibratePhone(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_CLOSE, R.anim.fade_in, R.anim.fade_out
                )
            } else {
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            finish()
        }

        binding.btAddCheckbox.setOnClickListener {
            vibratePhone(this)
            insertChecklist()
        }

        binding.btSaveNote.setOnClickListener {
            disableSaveButton()
            vibratePhone(this)

//            val note = createNote(userNameFrom)
            val note = createNote()
            mainViewModel.saveNoteConcurrently(note)

            finishWithAnimation()
        }


        mainViewModel.noteSavedMessage.observe(this) { message ->
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            Log.e("noteSavedMessage", "escucha noteSavedMessage")
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNote(/*userNameFrom: String?*/): Note {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val current = LocalDateTime.now().format(formatter)

        return Note(
            id = null,
            title = binding.etTitle.text.toString(),
            textContent = binding.noteContent.getPlainTextContent(),
            date = current.toString(),
//            userFrom = userNameFrom ?: "",
//            userTo = null,
            updatedTime = System.currentTimeMillis()
        )
    }

    private fun disableSaveButton() {
        binding.btSaveNote.apply {
            setBackgroundColor(this@WriteNotesActivity.getColor(R.color.disabled))
            isEnabled = false
            isClickable = false
        }
    }

    private fun finishWithAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.fade_in, R.anim.fade_out)
        } else {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        finish()
    }

    private fun insertChecklist() {
        try {
            mContentEditText.insertChecklist()
            mContentEditText.processChecklists()
            mContentEditText.setSelection(mContentEditText.length())
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
    }

    override fun onCheckboxToggled() {
        // Save note (using delay) after toggling a checkbox
    }

    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
        // Unused
    }

    override fun afterTextChanged(editable: Editable) {
        // Aplica el regex para checklists
        val updatedEditable = ChecklistUtils.addChecklistSpansForRegexAndColor(
            context = this,
            editable = editable,
            regex = ChecklistUtils.CHECKLIST_REGEX,
            color = R.color.green, // Asegúrate de tener un color definido
            isList = false // Cambia a `true` si necesitas estilo de lista
        )

        mContentEditText.removeTextChangedListener(this)  // Evita bucles de llamada recursiva
        mContentEditText.text = updatedEditable
        mContentEditText.addTextChangedListener(this)  // Restaura el TextWatcher después del cambio
    }

    override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
        // When text changes, start timer that will fire after AUTOSAVE_DELAY_MILLIS passes

        // Temporarily remove the text watcher as we process checklists to prevent callback looping
        mContentEditText.removeTextChangedListener(this)
        mContentEditText.processChecklists()
        mContentEditText.addTextChangedListener(this)
    }

    override fun onFocusChange(p0: View?, p1: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        TODO("Not yet implemented")
    }

    override fun onBackPressed() {
        super.onBackPressedDispatcher.onBackPressed()
        finishWithAnimation()
    }
}