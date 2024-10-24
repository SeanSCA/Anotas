package com.example.jinotas.utils

import android.app.ActivityManager
import android.content.Context
import android.provider.Settings
import com.example.jinotas.api.CrudApi
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

object Utils {
    // Método para obtener el ID del dispositivo
    fun getIdDevice(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun getAccessToken(context: Context): String {
        return withContext(Dispatchers.IO) {
            val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

            // Load the service account credentials from assets
            val inputStream = context.assets.open("notemanager-15064-6b4b2ba119a0.json")
            val credentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"))

            // Refresh the token if it's expired
            credentials.refreshIfExpired()

            // Return the access token
            credentials.accessToken.tokenValue
        }
    }

    fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName

        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }
}