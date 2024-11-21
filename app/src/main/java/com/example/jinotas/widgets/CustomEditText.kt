package com.example.jinotas.widgets

import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.util.Log
import android.view.ActionMode
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.AdapterView
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.ScrollView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.jinotas.R
import com.example.jinotas.databinding.PopupStylesBinding
import com.example.jinotas.utils.AppLog
import com.example.jinotas.utils.ChecklistUtils
import com.example.jinotas.utils.DisplayUtils
import com.example.jinotas.utils.DrawableUtils
import com.example.jinotas.utils.ThemeUtils
import com.example.jinotas.widgets.CustomLinkify.CUSTOM_LINK_ID
import com.example.jinotas.widgets.CustomLinkify.CUSTOM_LINK_PREFIX
import kotlinx.coroutines.delay
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CustomEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatEditText(context, attrs), AdapterView.OnItemClickListener {

    private val popupWindow: PopupWindow = PopupWindow(context)
    private lateinit var binding: PopupStylesBinding
    private var isPopupVisible: Boolean = false
    private lateinit var popupMenu: PopupMenu
    private val currentLocation = Point()
    private val startLocation = Point()
    private val currentBounds = Rect()
    private var targetTextViewTextSize: Float = 12f
    private var startIndex: Int? = null
    private var endIndex: Int? = null
    private var selectionStart: Int = -1
    private var selectionEnd: Int = -1
    private var span: Spannable? = null

    companion object {
        private val INTERNOTE_LINK_PATTERN_EDIT =
            Pattern.compile("([^]]*)(]\\($CUSTOM_LINK_PREFIX$CUSTOM_LINK_ID\\))")
        private val INTERNOTE_LINK_PATTERN_FULL =
            Pattern.compile("(?s)(.)*(\\[)$INTERNOTE_LINK_PATTERN_EDIT")
        private const val CHECKBOX_LENGTH = 2 // one ClickableSpan character + one space character
        const val DEFAULT_WIDTH = -1
        const val DEFAULT_HEIGHT = -1

        enum class ButtonType {
            HighLight, Bold, Italic, UnderLine
        }
    }

    @ColorRes
    private var highLightColor: Int = R.color.background

    init {
        movementMethod = LinkMovementMethod.getInstance()
        setUpPopupWindow()
        setTextIsSelectable(true)
        customSelectionActionModeCallback = OnSelectedCallback()
        setOnScrollChangeListener { _, _, _, _, _ ->
            if (hasSelection() && popupWindow.isShowing) {
                val (point, cX, cY) = getLocationAndCoordinate() ?: return@setOnScrollChangeListener
                val x = (cX + (point.x + cX))
                val y = (cY + (point.y - cY - scrollY))
                popupWindow.update(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT)
            }
        }
    }

    private fun dismissPopupWindow() {
        if (popupWindow != null) {
            popupWindow.dismiss()
        }
    }

    private lateinit var listeners: MutableList<OnSelectionChangedListener>/*= mutableListOf<OnSelectionChangedListener>()*/
    private var mOnCheckboxToggledListener: OnCheckboxToggledListener? = null

    fun addOnSelectionChangedListener(listener: OnSelectionChangedListener) {
        listeners.add(listener)
    }

    private fun shouldOverridePredictiveTextBehavior(): Boolean {
        val currentKeyboard = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
        )
        return Build.MANUFACTURER.equals(
            "samsung", ignoreCase = true
        ) && Build.VERSION.SDK_INT >= 33 && currentKeyboard?.startsWith("com.samsung.android.honeyboard") == true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val baseInputConnection = super.onCreateInputConnection(outAttrs)
        return if (shouldOverridePredictiveTextBehavior()) {
            AppLog.add(
                AppLog.Type.EDITOR, "Samsung keyboard detected, overriding predictive text behavior"
            )
            SamsungInputConnection(this, baseInputConnection!!)
        } else {
            baseInputConnection!!
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)

        if (::listeners.isInitialized) {
            listeners.forEach { it.onSelectionChanged(selStart, selEnd) }
        }

        startIndex = selStart
        endIndex = selEnd

        // Validar que los índices sean correctos antes de mostrar el popup
        if (selStart >= 0 && selEnd > selStart) {
            showPopupWindow() // Solo mostrar el popup si la selección es válida
        } else {
            dismissPopupWindow() // Ocultar el popup si no hay selección
        }
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
            clearFocus()
        }
        return super.onKeyPreIme(keyCode, event)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (focused) isCursorVisible = true
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    fun toggleCheckbox(checkableSpan: CheckableSpan) {
        isCursorVisible = false
        setSelection(length())

        val editable = text

        val checkboxStart = editable!!.getSpanStart(checkableSpan)
        val checkboxEnd = editable.getSpanEnd(checkableSpan)

        val selectionStart = selectionStart
        val selectionEnd = selectionEnd

        val imageSpans = editable.getSpans(
            checkboxStart, checkboxEnd, ImageSpan::class.java
        )

        if (imageSpans.isNotEmpty()) {
            val context = context
            // ImageSpans are static, so we need to remove the old one and replace :|
            @DrawableRes val resDrawable =
                if (checkableSpan.isChecked) R.drawable.ic_checkbox_editor_checked_24dp else R.drawable.ic_checkbox_editor_unchecked_24dp
            val iconDrawable: Drawable = DrawableUtils().tintDrawableWithAttribute(
                context,
                resDrawable,
                if (checkableSpan.isChecked) com.google.android.material.R.attr.colorAccent else com.google.android.material.R.attr.colorAccent
            )!!
            val iconSize: Int = DisplayUtils().getChecklistIconSize(context, false)
            iconDrawable.setBounds(0, 0, iconSize, iconSize)
            val newImageSpan = CenteredImageSpan(context, iconDrawable)
            Handler().post {
                editable.setSpan(
                    newImageSpan, checkboxStart, checkboxEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                editable.removeSpan(imageSpans[0])
                fixLineSpacing()

                // Restore the selection
                if (selectionStart >= 0 && selectionStart <= editable.length && selectionEnd <= editable.length && hasFocus()) {
                    setSelection(selectionStart, selectionEnd)
                    isCursorVisible = true
                }
                if (mOnCheckboxToggledListener != null) {
                    mOnCheckboxToggledListener!!.onCheckboxToggled()
                }
            }
        }
    }

    private fun findStartOfLineOfSelection(): Int {
        val position = selectionStart
        val editable = text
        return (position - 1 downTo 0).firstOrNull { editable!![it] == '\n' }?.plus(1) ?: 0
    }

    private fun findEndOfLineOfSelection(): Int {
        val position = maxOf(0, selectionEnd)
        val editable = text
        return (position until editable!!.length).firstOrNull { editable!!.get(it) == '\n' }
            ?.let { maxOf(it - 1, position) } ?: editable!!.length
    }

    fun insertChecklist() {
        val start = findStartOfLineOfSelection()
        val end = findEndOfLineOfSelection()

        val workingString = SpannableStringBuilder(text!!.subSequence(start, end))
        val editable = text

        if (editable!!.length < start || editable.length < end) return

        val previousSelection = selectionStart
        val checkableSpans =
            workingString.getSpans(0, workingString.length, CheckableSpan::class.java)
        if (checkableSpans.isNotEmpty()) {
            checkableSpans.forEach { span ->
                workingString.replace(
                    workingString.getSpanStart(span), workingString.getSpanEnd(span) + 1, ""
                )
                workingString.removeSpan(span)
            }

            editable.replace(start, end, workingString)

            if (checkableSpans.size == 1) {
                val newSelection = maxOf(previousSelection - CHECKBOX_LENGTH, 0)
                if (editable.length >= newSelection) setSelection(newSelection)
            }
        } else {
            val lines = workingString.toString().split("(?<=\n)".toRegex())
            val resultString = StringBuilder()

            lines.forEach { lineString ->
                val leadingSpaceCount = if (lineString.trim().isEmpty()) {
                    lineString.length - lineString.replace(" ", "").length
                } else {
                    lineString.indexOfFirst { it != ' ' }
                }

                if (leadingSpaceCount > 0) {
                    resultString.append(" ".repeat(leadingSpaceCount))
                }

                resultString.append(ChecklistUtils.UNCHECKED_MARKDOWN).append(" ")
                    .append(lineString.substring(leadingSpaceCount))
            }

            editable.replace(start, end, resultString)

            val newSelection = maxOf(previousSelection, 0) + (lines.size * CHECKBOX_LENGTH)
            if (editable.length >= newSelection) setSelection(newSelection)
        }
    }

    private fun selectionIsOnSameLine(): Boolean {
        val selectionStart = selectionStart
        val selectionEnd = selectionEnd
        val layout = layout ?: return false
        return layout.getLineForOffset(selectionStart) == layout.getLineForOffset(selectionEnd)
    }

    fun fixLineSpacing() {
        setLineSpacing(0f, 1f)
    }

    fun getPlainTextContent(): String {
        if (text == null) {
            return ""
        }

        val content = SpannableStringBuilder(text)
        val spans = content.getSpans(0, content.length, CheckableSpan::class.java)
        for (span in spans) {
            val start = content.getSpanStart(span)
            val end = content.getSpanEnd(span)
            (content as Editable).replace(
                start,
                end,
                if (span.isChecked) ChecklistUtils.CHECKED_MARKDOWN else ChecklistUtils.UNCHECKED_MARKDOWN
            )
        }

        return content.toString()
    }

    // Replaces any CheckableSpans with their markdown preview counterpart (e.g. '- [\u2a2f]')
    fun getPreviewTextContent(): String {
        if (text == null) {
            return ""
        }

        val content = SpannableStringBuilder(text)
        val spans = content.getSpans(0, content.length, CheckableSpan::class.java)
        for (span in spans) {
            val start = content.getSpanStart(span)
            val end = content.getSpanEnd(span)
            (content as Editable).replace(
                start,
                end,
                if (span.isChecked) ChecklistUtils.CHECKED_MARKDOWN_PREVIEW else ChecklistUtils.UNCHECKED_MARKDOWN
            )
        }

        return content.toString()
    }

    fun processChecklists() {
        if (text!!.isEmpty() || context == null) {
            return
        }

        try {
            ChecklistUtils.addChecklistSpansForRegexAndColor(
                context,
                text,
                ChecklistUtils.CHECKLIST_REGEX_LINES_CHECKED,
                ThemeUtils().getColorResourceFromAttribute(
                    context, com.onesignal.R.attr.colorAccent
                ),
                false
            )
            ChecklistUtils.addChecklistSpansForRegexAndColor(
                context,
                text,
                ChecklistUtils.CHECKLIST_REGEX_LINES_UNCHECKED,
                ThemeUtils().getColorResourceFromAttribute(
                    context, androidx.appcompat.R.attr.colorAccent
                ),
                false
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setOnCheckboxToggledListener(listener: OnCheckboxToggledListener?) {
        mOnCheckboxToggledListener = listener
    }

    interface OnCheckboxToggledListener {
        fun onCheckboxToggled()
    }

    interface OnSelectionChangedListener {
        fun onSelectionChanged(selStart: Int, selEnd: Int)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val start = max(selectionStart.toDouble(), 0.0).toInt()
        val end = max(selectionEnd.toDouble(), 0.0).toInt()
        editableText.replace(
            min(start.toDouble(), end.toDouble()).toInt(),
            max(start.toDouble(), end.toDouble()).toInt(),
            text,
            0,
            text!!.length
        )
    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        if (event.action == MotionEvent.ACTION_UP) {
//            if (popupWindow != null && !popupWindow.isShowing) {
//                showPopupWindow()
//                Log.e("aaaaaa", "Llegaaaa")
//            }
//        }
//        return super.onTouchEvent(event)
//    }

    private fun setUpPopupWindow() {
        popupWindow.apply {
            binding = PopupStylesBinding.inflate(LayoutInflater.from(context))
            width = context.resources.getDimensionPixelSize(R.dimen.popup_window_width)
            height = WRAP_CONTENT
            contentView = binding.root
            animationStyle = R.style.PopupWindowAnimationBottomSlide
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    context, R.drawable.bg_action_mode_popup_window
                )
            )
            isFocusable = false
            isOutsideTouchable = false

            binding.apply {
                imageButtonActionModePopupWindowClear.setOnClickListener {
                    onClear()
                }
                imageButtonActionModePopupWindowHighlight.setOnClickListener {
                    onClickImageButton(ButtonType.HighLight)
                }
                imageButtonActionModePopupWindowBold.setOnClickListener {
                    onClickImageButton(ButtonType.Bold)
                }
                imageButtonActionModePopupWindowItalic.setOnClickListener {
                    onClickImageButton(ButtonType.Italic)
                }
                imageButtonActionModePopupWindowUnderline.setOnClickListener {
                    onClickImageButton(ButtonType.UnderLine)
                }
            }
        }
    }

    private fun onClickImageButton(type: ButtonType) {
        post {
            val styleSpan = when (type) {
                ButtonType.HighLight -> BackgroundColorSpan(
                    ContextCompat.getColor(
                        context, highLightColor
                    )
                )

                ButtonType.Bold -> StyleSpan(Typeface.BOLD)
                ButtonType.Italic -> StyleSpan(Typeface.ITALIC)
                ButtonType.UnderLine -> UnderlineSpan()
            }
            span?.setSpan(
                styleSpan,
                startIndex ?: return@post,
                endIndex ?: return@post,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun onClear() {
        val spansToRemove = span?.getSpans(
            startIndex ?: return, endIndex ?: return, CharacterStyle::class.java
        ) ?: return
        for (s in spansToRemove) {
            span?.removeSpan(s)
        }
    }

    private fun showPopupWindow() {
        if (popupWindow.isShowing) {
            val (point, cX, cY) = getLocationAndCoordinate() ?: return
            if (cX >= 0 && cY >= 0) { // Validar las coordenadas
                popupWindow.update(cX + point.x, cY + point.y, DEFAULT_WIDTH, DEFAULT_HEIGHT)
            }
        } else {
            val (point, cX, cY) = getLocationAndCoordinate() ?: return
            if (cX >= 0 && cY >= 0) { // Validar las coordenadas
                popupWindow.showAtLocation(this, Gravity.TOP, cX + point.x, cY + point.y)
                popupWindow.isFocusable = false
                popupWindow.isOutsideTouchable = false
            }
        }
    }

    private fun getLocationAndCoordinate(): Triple<Point, Int, Int>? {
        val popLocation = calculatePopupLocation() ?: return null
        val currentX = currentLocation.x
        val currentY = currentLocation.y
        currentLocation.set(popLocation.x, popLocation.y)
        return Triple(popLocation, currentX, currentY)
    }

    private fun updatePopupWindow(point: Point, cX: Int, cY: Int) {
        popupWindow.update(cX + point.x, cY + point.y, DEFAULT_WIDTH, DEFAULT_HEIGHT)
    }

    private fun calculatePopupLocation(): Point? {
        val scrollView: ScrollView? = parent as? ScrollView

        val selectionStart = selectionStart
        val selectionEnd = selectionEnd
        val min = min(selectionStart, selectionEnd)
        val max = max(selectionStart, selectionEnd)

        val selectionBounds = RectF()
        val selection = Path()
        layout?.getSelectionPath(min, max, selection)
        selection.computeBounds(selectionBounds, true)

        val textPadding = paddingLeft
        val scrollY = scrollView?.scrollY ?: 0

        val x = (selectionBounds.centerX() + textPadding).roundToInt()
        val y = (selectionBounds.centerY() - scrollY).roundToInt()
        currentLocation.set(x, y)
        return currentLocation
    }

    fun setHighLightColor(@ColorRes colorRes: Int) {
        this.highLightColor = colorRes
    }

    private inner class OnSelectedCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = false
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false
        override fun onDestroyActionMode(mode: ActionMode) {
            dismissPopupWindow()
        }
    }
}