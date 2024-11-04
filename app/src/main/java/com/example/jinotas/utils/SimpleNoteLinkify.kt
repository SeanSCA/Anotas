package com.example.jinotas.utils

import android.app.Activity
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.util.Linkify
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import java.util.regex.Pattern

object SimplenoteLinkify {
    const val SIMPLENOTE_SCHEME: String = "simplenote://"
    const val SIMPLENOTE_LINK_PREFIX: String = SIMPLENOTE_SCHEME + "note/"
    const val SIMPLENOTE_LINK_ID: String = "([a-zA-Z0-9_\\.\\-%@]{1,256})"
    val SIMPLENOTE_LINK_PATTERN: Pattern = Pattern.compile(
        SIMPLENOTE_LINK_PREFIX + SIMPLENOTE_LINK_ID
    )

}