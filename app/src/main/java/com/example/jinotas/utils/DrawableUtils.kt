package com.example.jinotas.utils

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

class DrawableUtils {

    fun startAnimatedVectorDrawable(drawable: Drawable?) {
        if (drawable is AnimatedVectorDrawable) {
            drawable.start()
        } else if (drawable is AnimatedVectorDrawableCompat) {
            drawable.start()
        }
    }

    fun tintDrawableWithAttribute(
        context: Context?, @DrawableRes drawableRes: Int, @AttrRes tintColorAttribute: Int
    ): Drawable? {
        @ColorInt val color: Int = getColor(context!!, tintColorAttribute)
        return tintDrawable(
            context, drawableRes, color
        )
    }

    fun getColor(context: Context, @AttrRes tintColorAttribute: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(tintColorAttribute, typedValue, true)
        return typedValue.data
    }

    fun tintDrawable(
        context: Context?, @DrawableRes drawableRes: Int, @ColorInt color: Int
    ): Drawable? {
        return tintDrawable(
            ContextCompat.getDrawable(
                context!!, drawableRes
            ), color
        )
    }

    fun tintDrawable(drawable: Drawable?, @ColorInt color: Int): Drawable? {
        var drawable = drawable
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable).mutate()
            DrawableCompat.setTint(drawable, color)
        }
        return drawable
    }

    fun tintDrawableWithResource(
        context: Context?, drawable: Drawable?, @ColorRes colorRes: Int
    ): Drawable {
        @ColorInt val tint = ContextCompat.getColor(context!!, colorRes)
        return tintDrawable(drawable, tint)!!
    }

    fun tintMenuWithAttribute(context: Context?, menu: Menu?, @AttrRes tintColorAttribute: Int) {
        @ColorInt val color: Int = getColor(context!!, tintColorAttribute)
        tintMenu(menu!!, color)
    }

    fun tintMenu(menu: Menu, @ColorInt color: Int) {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            tintMenuItem(item, color)
        }
    }

    fun tintMenuWithResource(context: Context?, menu: Menu?, @ColorRes colorRes: Int) {
        @ColorInt val color = ContextCompat.getColor(context!!, colorRes)
        if (menu != null) {
            tintMenu(menu, color)
        }
    }

    fun tintMenuItem(menuItem: MenuItem, @ColorInt color: Int) {
        val tinted: Drawable = tintDrawable(menuItem.icon, color)!!
        menuItem.setIcon(tinted)
    }

    fun tintMenuItemWithResource(context: Context?, menuItem: MenuItem?, @ColorRes colorRes: Int) {
        @ColorInt val color = ContextCompat.getColor(context!!, colorRes)
        tintMenuItem(menuItem!!, color)
    }

    fun tintMenuItemWithAttribute(
        context: Context?, menuItem: MenuItem?, @AttrRes tintColorAttribute: Int
    ) {
        @ColorInt val color: Int = getColor(context!!, tintColorAttribute)
        tintMenuItem(menuItem!!, color)
    }
}