package com.example.jinotas.utils

import android.content.Context
import com.example.jinotas.R

val languages = arrayListOf(R.string.language_spanish, R.string.language_english)

fun getLanguageStrings(context: Context): List<String> {
    return languages.map { context.getString(it) }
}
