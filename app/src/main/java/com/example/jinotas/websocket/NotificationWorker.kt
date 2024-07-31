package com.example.jinotas.websocket

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.jinotas.MainActivity
import com.example.jinotas.R
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.wss
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams), WebSocketListener {

    private val channelId = "i.apps.notifications"
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }
    private var session: DefaultClientWebSocketSession? = null

    override fun doWork(): Result {
//        connectWebSocket()
        sendNotification()
        return Result.success()
    }

    private fun connectWebSocket() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.wss("wss://smallmintcat12.conveyor.cloud/api/websocket?nom=Sean") {
                    session = this
                    withContext(Dispatchers.Main) {
                        onConnected()
                    }
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            withContext(Dispatchers.Main) {
                                onMessage(frame.readText())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onDisconnected()
                }
            }
        }
    }

    private fun sendNotification() {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "channel_name"
            val channelDescription = "Notificaciones del canal"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notifyIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Notas")
            .setContentText("Nueva nota")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Se ha subido una nota nueva a la nube, recarga tus notas y la verás")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(notifyPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(1, notification)
        }
    }

    override fun onConnected() {
        Log.d("NotificationWorker", "WebSocket Connected")
    }

    override fun onMessage(message: String) {
        Log.d("NotificationWorker", "Message received: $message")
        if (message == "newNote") {
            sendNotification()
        }
    }

    override fun onDisconnected() {
        Log.d("NotificationWorker", "WebSocket Disconnected")
    }
}