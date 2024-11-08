package com.example.jinotas.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.example.jinotas.R

class PrefUtils {
    companion object {
        val PREF_STYLE_INDEX: String = "pref_key_style_index"
        val STYLE_DEFAULT: Int = 0
        const val PREF_FONT_SIZE: String = "pref_key_font_size"

        internal fun getPrefs(context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }

        fun getStyleIndexSelected(context: Context?): Int {
            return getPrefs(context!!).getInt(PREF_STYLE_INDEX, STYLE_DEFAULT)
        }

        fun getFontSize(context: Context?): Int {
            var defaultFontSize = 16
            // Just in case
            if (context == null) {
                return defaultFontSize
            }

            // Get default value for normal font size (differs based on screen/dpi size)
            defaultFontSize = context.resources.getInteger(R.integer.default_font_size)

            return getIntPref(context, PREF_FONT_SIZE, defaultFontSize)
        }

        fun getStringPref(context: Context?, prefKey: String?): String? {
            return getStringPref(context, prefKey, "")
        }

        fun getStringPref(context: Context?, prefKey: String?, defaultValue: String?): String? {
            return try {
                getPrefs(context!!).getString(prefKey, defaultValue)
            } catch (e: ClassCastException) {
                defaultValue
            }
        }

        fun getIntPref(context: Context?, prefKey: String?): Int {
            return getIntPref(context, prefKey, 0)
        }

        fun getIntPref(context: Context?, prefKey: String?, defaultValue: Int): Int {
            // read as string preference, then convert to int
            val strPref: String = getStringPref(context, prefKey, defaultValue.toString())!!
            return StrUtils().strToInt(strPref, defaultValue)
        }
    }
}