package com.example.jinotas

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.jinotas.custom_textview.CustomEditText
import com.example.jinotas.databinding.ActivityShowNoteBinding
import com.example.jinotas.db.Note
import com.example.jinotas.utils.ChecklistUtils
import com.example.jinotas.utils.Utils.vibratePhone
import com.example.jinotas.viewmodels.MainViewModel
import kotlinx.coroutines.Job
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ShowNoteActivity : AppCompatActivity(), TextWatcher, OnFocusChangeListener,
    CustomEditText.OnSelectionChangedListener, CustomEditText.OnCheckboxToggledListener {
    private lateinit var binding: ActivityShowNoteBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var notesShow: Note
    private var job: Job = Job()
    private lateinit var mContentEditText: CustomEditText

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowNoteBinding.inflate(layoutInflater)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val codeSearchUpdate = intent.getIntExtra("code", 0)
        val userName = intent.getStringExtra("userFrom")
        mContentEditText = binding.noteContent

        mainViewModel.getNoteByCode(codeSearchUpdate)

        mainViewModel.noteByCode.observe(this) { note ->
            Log.e("noteByCode", mainViewModel.noteByCode.value.toString())

            if (note != null) {
                Log.e("noteByCode", note.toString())
                notesShow = note
            }

            notesShow.let {
                binding.etTitle.setText(notesShow.title)
                binding.noteContent.setText(notesShow.textContent)
            }
            mContentEditText.processChecklists()
        }

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

        binding.btOverwriteNote.setOnClickListener {
            disableSaveButton()

            vibratePhone(this)

            val note = updateNote(userName, codeSearchUpdate)
            mainViewModel.overwriteNoteConcurrently(note)

            finishWithAnimation()
        }


        binding.btAddCheckbox.setOnClickListener {
            insertChecklist()
        }

        setContentView(binding.root)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateNote(userNameFrom: String?, codeSearchUpdate: Int): Note {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val current = LocalDateTime.now().format(formatter)

        return Note(
            id = notesShow.id,
            code = codeSearchUpdate,
            title = binding.etTitle.text.toString(),
            textContent = binding.noteContent.getPlainTextContent(),
            date = current,
            userFrom = userNameFrom!!,
            userTo = null
        )
    }

    private fun disableSaveButton() {
        binding.btOverwriteNote.apply {
            setBackgroundColor(this@ShowNoteActivity.getColor(R.color.disabled))
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
}