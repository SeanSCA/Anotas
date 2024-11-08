package com.example.jinotas.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.ExpandableListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.jinotas.DrawerExpandableListAdapter
import com.example.jinotas.R
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

object Utils {
    var lastClickTime: Long = 0
//    fun lastClickTime(): Long {
//        return 0
//    }

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

    fun setupExpandableListView(
        expandableListView: ExpandableListView,
        context: Context,
        drawerLayout: DrawerLayout // Nuevo parámetro
    ) {
        val titleList: List<String> = listOf(context.getString(R.string.change_language))
        val childMap = mapOf(
            context.getString(R.string.change_language) to getLanguageStrings(context)
        )

        val adapter = DrawerExpandableListAdapter(context, titleList, childMap)
        expandableListView.setAdapter(adapter)

        expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val group = titleList[groupPosition]
            val child = childMap[group]?.get(childPosition)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibratePhone(context)
                context.getSystemService(LocaleManager::class.java).applicationLocales =
                    LocaleList.forLanguageTags(child)
            } else {
                vibratePhone(context)
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(child)
                )
            }

            Toast.makeText(context, "Seleccionaste: $child", Toast.LENGTH_SHORT).show()

            drawerLayout.closeDrawer(GravityCompat.START)

            expandableListView.collapseGroup(groupPosition)

            true
        }
    }

    fun Activity.vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
    }

    fun Fragment.vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
    }

    fun Utils.vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
    }
}