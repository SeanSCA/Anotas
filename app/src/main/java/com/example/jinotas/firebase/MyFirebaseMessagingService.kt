package com.example.jinotas.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.jinotas.MainActivity
import com.example.jinotas.R
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Token
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessageService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle the received message and show a notification
        remoteMessage.notification?.let {
            sendNotification(it.body ?: "New Message")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val db: AppDatabase = AppDatabase.getDatabase(this@FirebaseMessageService)
        db.tokenDAO().updateToken(token = Token(token = token))
        Log.i("OnNewToken", "New Token -> $token")
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "Default"
        val notificationBuilder =
            NotificationCompat.Builder(this, channelId).setContentTitle("Note Manager")
                .setContentText(messageBody).setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true).setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android Oreo and above requires a notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}