package com.example.jinotas.utils

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.example.jinotas.R
import com.example.jinotas.widgets.CenteredImageSpan
import com.example.jinotas.widgets.CheckableSpan
import java.util.regex.Pattern

object ChecklistUtils {
    const val CHAR_BALLOT_BOX: Char = '\u2610'
    const val CHAR_BALLOT_BOX_CHECK: Char = '\u2611'
    const val CHAR_BULLET: Char = '\u2022'
    const val CHAR_NO_BREAK_SPACE: Char = '\u00a0'
    const val CHAR_VECTOR_CROSS_PRODUCT: Char = '\u2a2f'
    const val CHECKLIST_OFFSET: Int = 3

    var CHECKLIST_REGEX: String = "(\\s+)?(-[ \\t]+\\[[xX\\s]?\\])"
    var CHECKLIST_REGEX_LINES: String = "^(\\s+)?(-[ \\t]+\\[[xX\\s]?\\])"
    var CHECKLIST_REGEX_LINES_CHECKED: String = "^(\\s+)?(-[ \\t]+\\[[xX]?\\])"
    var CHECKLIST_REGEX_LINES_UNCHECKED: String = "^(\\s+)?(-[ \\t]+\\[[\\s]?\\])"
    var CHECKED_MARKDOWN_PREVIEW: String = "- [" + CHAR_VECTOR_CROSS_PRODUCT + "]"
    var CHECKED_MARKDOWN: String = "- [x]"
    var UNCHECKED_MARKDOWN: String = "- [ ]"

    /***
     * Adds CheckableSpans for matching markdown formatted checklists.
     *
     * @param context   [Context] from which to get the checkbox drawable, color, and size.
     * @param editable  [Editable] spannable string to match with the regular expression.
     * @param regex     [String] regular expression; CHECKLIST_REGEX or CHECKLIST_REGEX_LINES.
     * @param color     [Integer] resource id of the color to tint the checkbox.
     * @param isList    [Boolean] if checkbox is in list to determine size.
     *
     * @return          [Editable] spannable string with checkbox spans.
     */
    fun addChecklistSpansForRegexAndColor(
        context: Context?, editable: Editable?, regex: String?, color: Int, isList: Boolean
    ): Editable {
        if (editable == null) {
            return SpannableStringBuilder("")
        }

        val p = Pattern.compile(regex, Pattern.MULTILINE)
        val m = p.matcher(editable)
        var positionAdjustment = 0

        while (m.find()) {
            var start = m.start() - positionAdjustment
            val end = m.end() - positionAdjustment

            // Safety first!
            if (end > editable.length) {
                continue
            }

            val leadingSpaces = m.group(1)
            val match = m.group(2)

            if (!TextUtils.isEmpty(leadingSpaces)) {
                start += leadingSpaces.length
            }

            if (match == null) {
                continue
            }

            val checkableSpan: CheckableSpan = CheckableSpan()
            checkableSpan.updateCheckedState(match.contains("x") || match.contains("X"))
            editable.replace(start, end, CHAR_NO_BREAK_SPACE.toString())

            var iconDrawable = ContextCompat.getDrawable(
                context!!,
                if (checkableSpan.isChecked
                ) if (isList) R.drawable.ic_checkbox_list_checked_24dp else R.drawable.ic_checkbox_editor_checked_24dp
                else if (isList) R.drawable.ic_checkbox_list_unchecked_24dp else R.drawable.ic_checkbox_editor_unchecked_24dp
            )
            iconDrawable = DrawableUtils().tintDrawableWithResource(context, iconDrawable, color)
            val iconSize: Int = DisplayUtils().getChecklistIconSize(context, isList)
            iconDrawable!!.setBounds(0, 0, iconSize, iconSize)

            val imageSpan: CenteredImageSpan = CenteredImageSpan(context, iconDrawable)
            editable.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            editable.setSpan(checkableSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            positionAdjustment += (end - start) - 1
        }

        return editable
    }

    /***
     * Adds CheckableSpans for matching markdown formatted checklists.
     * @param editable the spannable string to run the regex against.
     * @param regex the regex pattern, use either CHECKLIST_REGEX or CHECKLIST_REGEX_LINES
     * @return Editable - resulting spannable string
     */
    fun addChecklistUnicodeSpansForRegex(editable: Editable?, regex: String?): Editable {
        if (editable == null) {
            return SpannableStringBuilder("")
        }

        val p = Pattern.compile(regex, Pattern.MULTILINE)
        val m = p.matcher(editable)

        var positionAdjustment = 0
        while (m.find()) {
            var start = m.start() - positionAdjustment
            val end = m.end() - positionAdjustment

            // Safety first!
            if (end > editable.length) {
                continue
            }

            val leadingSpaces = m.group(1)
            if (!TextUtils.isEmpty(leadingSpaces)) {
                start += leadingSpaces.length
            }

            val match = m.group(2) ?: continue

            val checkableSpan: CheckableSpan = CheckableSpan()
            checkableSpan.updateCheckedState(match.contains("x") || match.contains("X"))
            editable.replace(
                start,
                end,
                if (checkableSpan.isChecked) CHAR_BALLOT_BOX_CHECK.toString() else CHAR_BALLOT_BOX.toString()
            )
            editable.setSpan(checkableSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            positionAdjustment += (end - start) - 1
        }

        return editable
    }
}
