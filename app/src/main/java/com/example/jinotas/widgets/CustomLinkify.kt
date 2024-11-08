package com.example.jinotas.widgets

import android.text.Spannable
import android.text.SpannableString
import android.text.util.Linkify
import android.widget.TextView
import java.util.regex.Pattern

object CustomLinkify {
    const val CUSTOM_SCHEME: String = "jinotas://"
    const val CUSTOM_LINK_PREFIX: String = CUSTOM_SCHEME + "note/"
    const val CUSTOM_LINK_ID: String = "([a-zA-Z0-9_\\.\\-%@]{1,256})"

}
