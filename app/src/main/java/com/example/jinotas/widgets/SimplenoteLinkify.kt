package com.example.jinotas.widgets

import android.text.Spannable
import android.text.SpannableString
import android.text.util.Linkify
import android.widget.TextView
import java.util.regex.Pattern

object SimplenoteLinkify {
    const val SIMPLENOTE_SCHEME: String = "simplenote://"
    const val SIMPLENOTE_LINK_PREFIX: String = SIMPLENOTE_SCHEME + "note/"
    const val SIMPLENOTE_LINK_ID: String = "([a-zA-Z0-9_\\.\\-%@]{1,256})"
    val SIMPLENOTE_LINK_PATTERN: Pattern = Pattern.compile(
        SIMPLENOTE_LINK_PREFIX + SIMPLENOTE_LINK_ID
    )

    // Works the same as Linkify.addLinks, but doesn't set movement method
    fun addLinks(text: TextView, mask: Int): Boolean {
        if (mask == 0) {
            return false
        }

        val t = text.text

        if (t is Spannable) {
            val linked = Linkify.addLinks(t, mask)
            Linkify.addLinks(t, SIMPLENOTE_LINK_PATTERN, SIMPLENOTE_SCHEME)

            return linked
        } else {
            val s = SpannableString.valueOf(t)

            if (Linkify.addLinks(s, mask)) {
                text.text = s
                return true
            }

            return false
        }
    }

    fun getNoteLink(id: String): String {
        return "(" + SIMPLENOTE_LINK_PREFIX + id + ")"
    }

    fun getNoteLinkWithTitle(title: String, id: String): String {
        return "[" + title + "]" + getNoteLink(id)
    }

}
