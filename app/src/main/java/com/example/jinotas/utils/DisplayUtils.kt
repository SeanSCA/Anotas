package com.example.jinotas.utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.example.jinotas.R

class DisplayUtils {
    /**
     * Get the size of the checkbox drawable.
     *
     * @param context   [Context] from which to determine size of font plus checkbox extra.
     * @param isList    [Boolean] if checkbox is in list to determine size.
     *
     * @return          [Integer] value of checkbox in pixels.
     */
    fun getChecklistIconSize(context: Context, isList: Boolean): Int {
        val extra = context.resources.getInteger(R.integer.default_font_size_checkbox_extra)
        val size: Int = getFontSize(context)
        return dpToPx(context, if (isList) size else size + extra)
    }

    fun getFontSize(context: Context?): Int {
        var defaultFontSize = 16
        // Just in case
        if (context == null) {
            return defaultFontSize
        }

        // Get default value for normal font size (differs based on screen/dpi size)
        defaultFontSize = context.resources.getInteger(R.integer.default_font_size)

        return getIntPref(context, PrefUtils.PREF_FONT_SIZE, defaultFontSize)
    }

    fun dpToPx(context: Context, dp: Int): Int {
        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            context.resources.displayMetrics
        )
        return px.toInt()
    }

    fun getIntPref(context: Context?, prefKey: String?, defaultValue: Int): Int {
        // read as string preference, then convert to int
        val strPref: String = getStringPref(context, prefKey, defaultValue.toString())!!
        return StrUtils().strToInt(strPref, defaultValue)
    }

    fun getStringPref(context: Context?, prefKey: String?, defaultValue: String?): String? {
        return try {
            PrefUtils.getPrefs(context!!).getString(prefKey, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    /**
     * Hides the keyboard for the given [View].  Since no [InputMethodManager] flag is
     * used, the keyboard is forcibly hidden regardless of the circumstances.
     */
    fun hideKeyboard(view: View?) {
        if (view == null) {
            return
        }

        val inputMethodManager =
            view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Shows the keyboard for the given [View].  Since a [InputMethodManager] flag is
     * used, the keyboard is implicitly shown regardless of the user request.
     */
    fun showKeyboard(view: View?) {
        if (view == null) {
            return
        }

        val inputMethodManager =
            view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}