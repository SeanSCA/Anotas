package com.example.jinotas

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import com.example.jinotas.db.Note
import com.example.jinotas.utils.AppLog
import com.example.jinotas.utils.DisplayUtils
import com.example.jinotas.utils.PrefUtils
import com.example.jinotas.utils.SimplenoteMovementMethod
import com.example.jinotas.widgets.SimplenoteEditText
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max

@AndroidEntryPoint
class NoteEditorFragment() : Fragment(), TextWatcher, OnFocusChangeListener,
    SimplenoteEditText.OnSelectionChangedListener, SimplenoteEditText.OnCheckboxToggledListener {
    private var mNote: Note? = null
    private var mRootView: View? = null
    private var mContentEditText: SimplenoteEditText? = null
    private var mHistoryTimeoutHandler: Handler? = null
    private var mMatchOffsets: String? = null
    private var mCurrentCursorPosition = 0
    private var mIsPaused = false

    // Hides the history bottom sheet if no revisions are loaded
    private val mHistoryTimeoutRunnable: Runnable = Runnable {
        if (!isAdded) {
            return@Runnable
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.add(AppLog.Type.SCREEN, "Created (NoteEditorFragment)")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        mRootView = inflater.inflate(R.layout.fragment_note_editor, container, false)
        mContentEditText = mRootView!!.findViewById(R.id.note_content)
        mContentEditText!!.addOnSelectionChangedListener(this)
        mContentEditText!!.setOnCheckboxToggledListener(this)
        mContentEditText!!.movementMethod = SimplenoteMovementMethod.instance
        mContentEditText!!.onFocusChangeListener = this
        mContentEditText!!.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, PrefUtils.getFontSize(requireContext()).toFloat()
        )

        setHasOptionsMenu(true)
        return mRootView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // If the user changes configuration and is still traversing keywords, we need to keep the scroll to the last
        // keyword checked
        if (mMatchOffsets != null) {
            // mContentEditText.getLayout() can be null after a configuration change, thus, we need to check when the
            // layout becomes available so that the scroll position can be set.
            mRootView!!.viewTreeObserver.addOnPreDrawListener(object :
                ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (mContentEditText!!.getLayout() != null) {
                        mRootView!!.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    return true
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        mIsPaused = false

        if (mContentEditText != null) {
            mContentEditText!!.setTextSize(
                TypedValue.COMPLEX_UNIT_SP, PrefUtils.getFontSize(requireContext()).toFloat()
            )

            if (mContentEditText!!.hasFocus()) {
                showSoftKeyboard()
            }
        }
    }

    private fun showSoftKeyboard() {
        Handler().postDelayed({
            if (getActivity() == null) {
                return@postDelayed
            }
            val inputMethodManager: InputMethodManager? =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(mContentEditText, 0)
            }
        }, 100)
    }

    override fun onPause() {
        super.onPause() // Always call the superclass method first
        mIsPaused = true

        // Hide soft keyboard if it is showing...
        DisplayUtils().hideKeyboard(mContentEditText)

        if (mHistoryTimeoutHandler != null) {
            mHistoryTimeoutHandler!!.removeCallbacks(mHistoryTimeoutRunnable)
        }

        AppLog.add(AppLog.Type.SCREEN, "Paused (NoteEditorFragment)")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLog.add(AppLog.Type.SYNC, "Removed note bucket listener (NoteEditorFragment)")
        AppLog.add(AppLog.Type.SCREEN, "Destroyed (NoteEditorFragment)")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    fun insertChecklist() {

        try {
            mContentEditText!!.insertChecklist()
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
                mNote!!.textContent, noteContentString, mContentEditText!!.getSelectionEnd()
            )
            mContentEditText!!.setText(mNote!!.textContent)
            // Set the scroll position after the note's content has been rendered

            if (isNoteUpdate) {
                if ((mContentEditText!!.hasFocus() && cursorPosition != mContentEditText!!.getSelectionEnd()) && cursorPosition < mContentEditText!!.getText()!!.length) {
                    mContentEditText!!.setSelection(cursorPosition)
                }
            }

            afterTextChanged(mContentEditText!!.getText()!!)
            mContentEditText!!.processChecklists()
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
        attemptAutoList(editable)
        setTitleSpan(editable)
        mContentEditText!!.fixLineSpacing()
    }

    override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
        // When text changes, start timer that will fire after AUTOSAVE_DELAY_MILLIS passes

        // Temporarily remove the text watcher as we process checklists to prevent callback looping
        mContentEditText!!.removeTextChangedListener(this)
        mContentEditText!!.processChecklists()
        mContentEditText!!.addTextChangedListener(this)
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
        mCurrentCursorPosition = mContentEditText!!.selectionStart
//        AutoBullet.apply(editable, oldCursorPosition, mCurrentCursorPosition)
        mCurrentCursorPosition = mContentEditText!!.selectionStart
    }

    private val noteContentString: String
        get() {
            if (mContentEditText == null || mContentEditText!!.getText() == null) {
                return ""
            } else {
                return mContentEditText!!.getText().toString()
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
}
