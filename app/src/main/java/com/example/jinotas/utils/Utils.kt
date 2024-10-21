package com.example.jinotas.utils

import android.content.Context
import android.provider.Settings

object Utils {
    // Método para obtener el ID del dispositivo
    fun getIdDevice(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}