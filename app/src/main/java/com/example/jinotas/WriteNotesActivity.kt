package com.example.jinotas

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.commonsware.cwac.anddown.AndDown
import com.example.jinotas.adapter.AdapterNotes
import com.example.jinotas.api.CrudApi
import com.example.jinotas.databinding.ActivityWriteNotesBinding
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.utils.ChecklistUtils
import com.example.jinotas.utils.Utils
import com.example.jinotas.utils.Utils.vibratePhone
import com.example.jinotas.widgets.CustomEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext
import kotlin.math.max


class WriteNotesActivity : AppCompatActivity(), CoroutineScope, TextWatcher, OnFocusChangeListener,
    CustomEditText.OnSelectionChangedListener, CustomEditText.OnCheckboxToggledListener {
    private lateinit var binding: ActivityWriteNotesBinding
    private lateinit var notesList: ArrayList<Note>
    private var mNote: Note? = null
    private lateinit var adapterNotes: AdapterNotes
    private lateinit var db: AppDatabase
    private var job: Job = Job()
    private lateinit var andDown: AndDown
    private var canConnect: Boolean = false
    private var mCurrentCursorPosition = 0

    //Markdown
    private lateinit var mContentEditText: CustomEditText
    private val mAutoSaveHandler: Handler? = null
    private val mCss: String? = null
    private val mRootView: View? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userNameFrom = intent.getStringExtra("userFrom")
        andDown = AndDown()
//        setupWebView()
        val inflater = this.layoutInflater
//        mRootView = inflater.inflate()
        mContentEditText = binding.noteContent

        binding.btReturnToNotes.setOnClickListener {
            vibratePhone(this)
            finish()
        }

        binding.btAddCheckbox.setOnClickListener {
            vibratePhone(this)
            insertChecklist()
//            mContentEditText.dissmisPopup()

        }

        binding.btSaveNote.setOnClickListener {
            binding.btSaveNote.setBackgroundColor(this.getColor(R.color.disabled))
            binding.btSaveNote.isEnabled = false
            binding.btSaveNote.isClickable = false

            vibratePhone(this)

            lifecycleScope.launch {
                delay(1)

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val current = LocalDateTime.now().format(formatter)

                val note = Note(
                    id = null,
                    title = binding.etTitle.text.toString(),
                    textContent = binding.noteContent.getPlainTextContent(),
                    date = current.toString(),
                    userFrom = userNameFrom ?: "",
                    userTo = null
                )
                db = AppDatabase.getDatabase(this@WriteNotesActivity)
                db.noteDAO().insertNote(note)
                notesList = db.noteDAO().getNotesList() as ArrayList<Note>
                adapterNotes = AdapterNotes(notesList, coroutineContext)
                adapterNotes.updateList(notesList)
                uploadNoteApi(note)
            }
            finish()
        }

        binding.btUpperLower.setOnClickListener {
            Utils.setUpperLowerCase(binding.noteContent, this)
        }

//        binding.noteContent.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                if (binding.noteContent.hasFocus() && binding.noteContent.selectionStart == binding.noteContent.selectionEnd) {
//                    // Reaplicar la selección si se pierde
//                    binding.noteContent.setSelection(0, s?.length ?: 0)
//                }
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })
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

    private fun refreshContent(isNoteUpdate: Boolean) {
        if (mNote != null) {
            // Restore the cursor position if possible.
            val cursorPosition = newCursorLocation(
                mNote!!.textContent, noteContentString, mContentEditText.selectionEnd
            )
            mContentEditText.setText(mNote!!.textContent)
            // Set the scroll position after the note's content has been rendered

            if (isNoteUpdate) {
                if ((mContentEditText.hasFocus() && cursorPosition != mContentEditText.selectionEnd) && cursorPosition < mContentEditText.getText()!!.length) {
                    mContentEditText.setSelection(cursorPosition)
                }
            }

            afterTextChanged(mContentEditText.getText()!!)
            mContentEditText.processChecklists()
        }
    }

    private fun newCursorLocation(newText: String, oldText: String, cursorLocation: Int): Int {
        // Ported from the iOS app :)
        // Cases:
        // 0. All text after cursor (and possibly more) was removed ==> put cursor at end
        // 1. Text was added after the cursor ==> no change
        // 2. Text was added before the cursor ==> location advances
        // 3. Text was removed after the cursor ==> no change
        // 4. Text was removed before the cursor ==> location retreats
        // 5. Text was added/removed on both sides of the cursor ==> not handled

        var cursorLocation = cursorLocation
        cursorLocation = max(cursorLocation.toDouble(), 0.0).toInt()

        var newCursorLocation = cursorLocation

        val deltaLength = newText.length - oldText.length

        // Case 0
        if (newText.length < cursorLocation) return newText.length

        var beforeCursorMatches = false
        var afterCursorMatches = false

        try {
            beforeCursorMatches =
                oldText.substring(0, cursorLocation) == newText.substring(0, cursorLocation)
            afterCursorMatches =
                oldText.substring(cursorLocation) == newText.substring(cursorLocation + deltaLength)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Cases 2 and 4
        if (!beforeCursorMatches && afterCursorMatches) newCursorLocation += deltaLength

        // Cases 1, 3 and 5 have no change
        return newCursorLocation
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

    /**
     * Set the note title to be a larger size and bold style.
     *
     * Remove all existing spans before applying spans or performance issues will occur.  Since both
     * [RelativeSizeSpan] and [StyleSpan] inherit from [MetricAffectingSpan], all
     * spans are removed when [MetricAffectingSpan] is removed.
     */
    private fun setTitleSpan(editable: Editable) {
        for (span: MetricAffectingSpan in editable.getSpans(
            0, editable.length, MetricAffectingSpan::class.java
        )) {
            if (span is RelativeSizeSpan || span is StyleSpan) {
                editable.removeSpan(span)
            }
        }

        val newLinePosition = noteContentString.indexOf("\n")

        if (newLinePosition == 0) {
            return
        }

        val titleEndPosition = if ((newLinePosition > 0)) newLinePosition else editable.length
        editable.setSpan(
            RelativeSizeSpan(1.3f), 0, titleEndPosition, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        editable.setSpan(
            StyleSpan(Typeface.BOLD), 0, titleEndPosition, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }

    private fun attemptAutoList(editable: Editable) {
        val oldCursorPosition = mCurrentCursorPosition
        mCurrentCursorPosition = mContentEditText.selectionStart
        mCurrentCursorPosition = mContentEditText.selectionStart
    }

    private val noteContentString: String
        get() {
            return if (mContentEditText.getText() == null) {
                ""
            } else {
                mContentEditText.getText().toString()
            }
        }

    companion object {
        val ARG_IS_FROM_WIDGET: String = "is_from_widget"
        val ARG_ITEM_ID: String = "item_id"
        val ARG_NEW_NOTE: String = "new_note"
        val ARG_MATCH_OFFSETS: String = "match_offsets"
        val ARG_MARKDOWN_ENABLED: String = "markdown_enabled"
        val ARG_PREVIEW_ENABLED: String = "preview_enabled"

        private val STATE_NOTE_ID = "state_note_id"
        private val AUTOSAVE_DELAY_MILLIS = 2000
        private val MAX_REVISIONS = 30
        private val PUBLISH_TIMEOUT = 20000
        private val HISTORY_TIMEOUT = 10000
    }

    override fun onFocusChange(p0: View?, p1: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        TODO("Not yet implemented")
    }

    private fun uploadNoteApi(notePost: Note) {
        if (tryConnection()) {
            lifecycleScope.launch {
                CrudApi().postNote(notePost, this@WriteNotesActivity)
                Toast.makeText(this@WriteNotesActivity, "Has subido la nota", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun tryConnection(): Boolean {
        try {
            canConnect = CrudApi().canConnectToApi()
        } catch (e: Exception) {
            Log.e("cantConnectToApi", "No tienes conexión con la API")
        }
        return canConnect
    }
}