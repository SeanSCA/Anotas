package com.example.jinotas.websocket

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.jinotas.MainActivity
import com.example.jinotas.R
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WebSocketService : LifecycleService(), WebSocketListener {
    val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }
    private val webSocketClient = WebSocketClient(dotenv["WEB_SOCKET_CLIENT"], lifecycleScope)
    private val CHANNEL_ID = "WebSocketServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification =
            NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Conexión exitosa")
                .setContentText("Preparado para las notas").setSmallIcon(R.drawable.app_icon)
                .setContentIntent(pendingIntent).build()

        startForeground(1, notification)

        lifecycleScope.launch(Dispatchers.Main) {
            webSocketClient.connect(this@WebSocketService)
        }
    }

    override fun onConnected() {
    }

    override fun onMessage(message: String) {
        if (message == "newNote") {
            val workRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java).build()
            WorkManager.getInstance(this).enqueue(workRequest)
        }
    }

    override fun onDisconnected() {
//        lifecycleScope.launch(Dispatchers.Main) {
//            webSocketClient.connect(this@WebSocketService)
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            webSocketClient.disconnect()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "WebSocket Service Channel", NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}