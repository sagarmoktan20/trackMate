package com.example.trackmate1

import android.util.Log
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
        // Handle incoming messages here (for when app is in foreground)
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
} 