package com.example.jinotas.utils

import android.R
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.AttrRes

class ThemeUtils {
    val STYLE_BLACK: Int = 2
    val STYLE_CLASSIC: Int = 1
    val STYLE_DEFAULT: Int = 0
    val STYLE_MONO: Int = 4
    val STYLE_PUBLICATION: Int = 5
    val STYLE_MATRIX: Int = 3
    val STYLE_SEPIA: Int = 6

    val STYLE_ARRAY: IntArray = intArrayOf(
        STYLE_DEFAULT,
        STYLE_CLASSIC,
        STYLE_BLACK,
        STYLE_MATRIX,
        STYLE_MONO,
        STYLE_PUBLICATION,
        STYLE_SEPIA
    )
    fun getCssFromStyle(context: Context?): String {
        val isLight: Boolean = isLightTheme(context!!)

        return when (PrefUtils.getStyleIndexSelected(context)) {
            STYLE_BLACK -> if (isLight) "light_black.css" else "dark_black.css"
            STYLE_CLASSIC -> if (isLight) "light_classic.css" else "dark_classic.css"
            STYLE_MATRIX -> if (isLight) "light_matrix.css" else "dark_matrix.css"
            STYLE_MONO -> if (isLight) "light_mono.css" else "dark_mono.css"
            STYLE_PUBLICATION -> if (isLight) "light_publication.css" else "dark_publication.css"
            STYLE_SEPIA -> if (isLight) "light_sepia.css" else "dark_sepia.css"
            STYLE_DEFAULT -> if (isLight) "light_default.css" else "dark_default.css"
            else -> if (isLight) "light_default.css" else "dark_default.css"
        }
    }

    fun isLightTheme(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
    }

    fun getColorFromAttribute(context: Context, @AttrRes attribute: Int): Int {
        return context.getColor(getColorResourceFromAttribute(context, attribute))
    }

    fun getColorResourceFromAttribute(context: Context, @AttrRes attribute: Int): Int {
        val typedArray = context.obtainStyledAttributes(intArrayOf(attribute))
        val colorResId = typedArray.getResourceId(0, R.color.black)
        typedArray.recycle()
        return colorResId
    }
}