package com.example.jinotas.api

import android.Manifest
import android.R.id.message
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.jinotas.MainActivity
import com.example.jinotas.R
import com.example.jinotas.db.Note
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class NotificacionProgramada(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    private val channelId = "i.apps.notifications"
    private val notificationPermissionCode = 250
    private var canConnect: Boolean = false

    override fun doWork(): Result {
        return try {
            val crudApi = CrudApi()
            val oldNotesList = crudApi.getNotesList() ?: emptyList()
            Thread.sleep(1000)  // Simulate delay if needed
            val newNotesList = crudApi.getNotesList() ?: emptyList()

            if (oldNotesList != newNotesList) {
                Log.i("NotificacionProgramada", "Nuevas notas detectadas")
                sendNotification("Nueva nota", "Se ha añadido una nueva nota.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("NotificacionProgramada", "Error en la tarea de trabajo", e)
            Result.failure()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Notificaciones"
            val channelDescription = "Canal para notificaciones de la aplicación"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.app_icon).setContentTitle(title).setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build()

        if (ActivityCompat.checkSelfPermission(
                this@NotificacionProgramada.applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(1, notification)
        }
    }
}