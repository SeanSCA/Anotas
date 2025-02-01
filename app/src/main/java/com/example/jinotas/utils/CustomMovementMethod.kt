package com.example.jinotas.utils

import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.view.MotionEvent
import android.widget.TextView
import com.example.jinotas.custom_textview.CheckableSpan

class CustomMovementMethod : ArrowKeyMovementMethod() {
    override fun onTouchEvent(textView: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        var x = event.x.toInt()
        var y = event.y.toInt()

        x -= textView.totalPaddingLeft
        y -= textView.totalPaddingTop

        x += textView.scrollX
        y += textView.scrollY

        val layout = textView.layout
        val line = layout.getLineForVertical(y)
        var off = layout.getOffsetForHorizontal(line, x.toFloat())
        val lineStart = layout.getLineStart(line)

        // Also toggle the checkbox if the user tapped the space next to the checkbox
        if (off == lineStart + 1) {
            off = lineStart
        }

        val checkableSpans: Array<CheckableSpan> =
            buffer.getSpans(off, off, CheckableSpan::class.java)

        if (checkableSpans.size != 0) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> if (!textView.hasFocus()) {
                    textView.isFocusableInTouchMode = false
                }

                MotionEvent.ACTION_UP -> {
                    checkableSpans[0].onClick(textView)
                    textView.isFocusableInTouchMode = true
                }
            }
            return true
        }

        return false
    }

    companion object {
        private var mInstance: CustomMovementMethod? = null

        val instance: CustomMovementMethod?
            get() {
                if (mInstance == null) {
                    mInstance = CustomMovementMethod()
                }

                return mInstance
            }
    }
}
