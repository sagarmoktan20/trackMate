package com.example.trackmate1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "New FCM token: $token")
        saveFcmTokenToFirestore(token)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_MESSAGE", "Message received: ${remoteMessage.data}")
        // Show notification even if app is in foreground
        showNotification(remoteMessage)
    }
    
    private fun saveFcmTokenToFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.email ?: return)
                .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN", "FCM token saved to Firestore successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_TOKEN", "Failed to save FCM token: ${e.message}")
                }
        }
    }

    private fun showNotification(remoteMessage: RemoteMessage) {
        val channelId = "task_channel"
        val channelName = "Task Notifications"
        val notificationId = (remoteMessage.data["taskId"]?.hashCode() ?: System.currentTimeMillis().toInt())

        // Extract title and body from notification or data
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "New Task Assigned"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "You have a new task."

        // Create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }
} 