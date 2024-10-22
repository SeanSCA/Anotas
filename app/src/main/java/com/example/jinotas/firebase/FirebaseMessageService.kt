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
import com.example.jinotas.db.Note
import com.example.jinotas.db.Token
import com.example.jinotas.utils.Utils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessageService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Verificar si el mensaje contiene datos personalizados
        if (remoteMessage.data.isNotEmpty()) {
            val receivedDeviceId = remoteMessage.data["deviceId"]
            val currentDeviceId = Utils.getIdDevice(context = this@FirebaseMessageService)

            if (receivedDeviceId != currentDeviceId) {
                // Crear manualmente el objeto Note a partir de los datos recibidos
                val note = Note(
                    code = remoteMessage.data["code"]?.toInt() ?: 0,
                    id = remoteMessage.data["id"]?.toIntOrNull(),
                    title = remoteMessage.data["title"] ?: "Sin título",
                    textContent = remoteMessage.data["textContent"] ?: "",
                    date = remoteMessage.data["date"] ?: "",
                    userFrom = remoteMessage.data["userFrom"] ?: "Desconocido",
                    userTo = remoteMessage.data["userTo"],
                    createdAt = remoteMessage.data["createdAt"],
                    updatedAt = remoteMessage.data["updatedAt"]
                )

                // Mostrar la notificación usando los datos de la nota
                sendNotificationNewNote(note.title, note.textContent)
            }
        }
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val db: AppDatabase = AppDatabase.getDatabase(this@FirebaseMessageService)
        db.tokenDAO().updateToken(token = Token(token = token))
        Log.i("OnNewToken", "New Token -> $token")
    }

    private fun sendNotificationNewNote(title: String, body: String) {
        Log.e("title", title)
        Log.e("body", body)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "Default"
        val notificationBuilder =
            NotificationCompat.Builder(this, channelId).setContentTitle(title).setContentText(body)
                .setSmallIcon(R.drawable.ic_notification).setAutoCancel(true)
                .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}