package com.example.trackmate1.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackmate1.ui.theme.Trackmate1Theme
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Date


@Composable
fun SearchScreen() {
    var searchText by remember { mutableStateOf("") }
    val invitationList = remember { mutableStateListOf<String>() } // placeholder list
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        getInvites(invitationList)
        startAutoLocationSharing(context)
        startAutoWorkingStatusListeners()
        startOnlineStatusMonitor()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        // 🔍 Search Bar and Send Button
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter email") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {

                sendInvite(searchText)
//                if (searchText.isNotBlank()) {
//                    invitationList.add(searchText)
//                    searchText = ""
//                }
            },colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                Text("Send")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 📌 "Your Invites:"
        Text(
            text = "Your Invites:",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 📋 List of Invitations
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(invitationList) { email ->
                InvitationItem(email = email,context = context)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
var inviteListener: ListenerRegistration? = null

fun getInvites(invitationList: SnapshotStateList<String>) {
    val firestoreDb = Firebase.firestore
    inviteListener?.remove() // Remove previous listener if any to prevent leaks

   firestoreDb.collection("users").document(FirebaseAuth.getInstance()
        .currentUser?.email.toString()).collection("invites")
       .addSnapshotListener { snapshot, e ->
           if (e != null) {
               return@addSnapshotListener
           }
           if (snapshot != null) {
               invitationList.clear()
               Log.d("firestore", "Current data: ${snapshot.documents}")
               for (item in snapshot) {
                   if (item.get("invite_status") == 0L) {
                       invitationList.add(item.id)
                   }
               }
           }
       }



//        .get().addOnCompleteListener() {
//            if(it.isSuccessful) {
//
//                for(item in it.result){
//                    if(item.get("invite_status") == 0L){
//                   invitationList.add(item.id)
//                    }
//                }
//            }
//
//
//        }
}

fun sendInvite(searchText: String) {
 val mailFromSearchBar = searchText;
    val firestoreDb = Firebase.firestore
    val data = hashMapOf(
        "invite_status" to 0
    )
    val myMail = FirebaseAuth.getInstance().currentUser?.email.toString();
    firestoreDb.collection("users")
        .document(mailFromSearchBar).collection("invites")
        .document(myMail).set(data).addOnSuccessListener {}.addOnFailureListener {}
}

@Composable
fun InvitationItem(email: String,context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accept Button
            Button(
                onClick = {
                    val firestoreDb = Firebase.firestore
                    val myEmail = FirebaseAuth.getInstance().currentUser?.email.toString()
                    
                    // First update the invite status
                    firestoreDb.collection("users")
                        .document(myEmail)
                        .collection("invites")
                        .document(email)
                        .update("invite_status", 1)
                        .addOnSuccessListener {
                            // Get current working status, name, and imageUrl of the receiver
                            firestoreDb.collection("users")
                                .document(myEmail)
                                .get()
                                .addOnSuccessListener { userDocument ->
                                    val currentWorkingStatus = userDocument.getString("workingStatus") ?: "Free"
                                    val receiverName = userDocument.getString("Name") ?: ""
                                    val receiverImageUrl = userDocument.getString("imageUrl") ?: ""
                                    
                                    // After successful status update, create shared_locations document in sender's collection
                                    val sharedLocationData = hashMapOf(
                                        "status" to "active",
                                        "shared_by" to myEmail,
                                        "working_status" to currentWorkingStatus,
                                        "receiver_name" to receiverName,
                                        "receiver_imageUrl" to receiverImageUrl,
                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                        "online_status" to true // Set online_status to true initially
                                    )
                                    
                                    // Create the shared_locations document in the sender's collection
                                    firestoreDb.collection("users")
                                        .document(email)  // This is the sender's document
                                        .collection("shared_locations")
                                        .document(myEmail)  // This is the receiver's email
                                        .set(sharedLocationData)
                                        .addOnSuccessListener {
                                            Log.d("Firestore", "Successfully created shared_locations document with working status, name, and imageUrl")
                                            
                                            // Set up global working status listener for this user
                                            setupWorkingStatusListener(myEmail, email)
                                            // No need to start FusedLocationProviderClient here; MainActivity handles location updates
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Firestore", "Error creating shared_locations document", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error getting user document for working status", e)
                                }
                        }
                },
                modifier = Modifier.width(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                Text("Accept")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Reject Button
            Button(
                onClick = { 
                    val firestoreDb = Firebase.firestore
                    firestoreDb.collection("users")
                        .document(FirebaseAuth.getInstance().currentUser?.email.toString())
                        .collection("invites")
                        .document(email)
                        .update("invite_status", -1)
                },
                modifier = Modifier.width(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Reject")
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Email Placeholder
            Text(
                text = email,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}

// Global working status listener to update all shared_locations documents
private fun setupWorkingStatusListener(userEmail: String, sharedWithEmail: String) {
    val firestoreDb = Firebase.firestore
    
    // Listen for working status changes in the user's profile
    firestoreDb.collection("users")
        .document(userEmail)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Firestore101", "Error listening for working status changes", e)
                return@addSnapshotListener
            }

            snapshot?.let { doc ->
                val newWorkingStatus = doc.getString("workingStatus") ?: "Free"
                
                // Update the working status in all shared_locations documents where this user is sharing
                firestoreDb.collection("users")
                    .document(sharedWithEmail)  // The user who is receiving the shared location
                    .collection("shared_locations")
                    .document(userEmail)  // The user who is sharing their location
                    .update("working_status", newWorkingStatus)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Updated working status to: $newWorkingStatus for user: $userEmail")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error updating working status", e)
                    }
            }
        }
}

fun updateSharedLocation(sharerEmail: String, viewerEmail: String) {
    val firestoreDb = Firebase.firestore
    firestoreDb.collection("users")
        .document(sharerEmail)
        .get()
        .addOnSuccessListener { sharerDoc ->
            val latitude = sharerDoc.getDouble("latitude")
            val longitude = sharerDoc.getDouble("longitude")
            if (latitude != null && longitude != null) {
                firestoreDb.collection("users")
                    .document(viewerEmail)
                    .collection("shared_locations")
                    .document(sharerEmail)
                    .update(
                        mapOf(
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                    )
            }
        }
}

fun startAutoLocationSharing(context: Context) {
    val firestoreDb = Firebase.firestore
    val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
    Log.d("myemail", "startAutoLocationSharing: $myEmail")
    firestoreDb.collection("users")
        .document(myEmail)
        .collection("invites")
        .whereEqualTo("invite_status", 1)
        .get()
        .addOnSuccessListener { invitesSnapshot ->
            for (inviteDoc in invitesSnapshot) {
                val senderEmail = inviteDoc.id
                // Update sender's shared_locations with my (receiver's) coordinates
                updateSharedLocation(myEmail, senderEmail)
            }
        }
    // Also check if this user has sent invites that were accepted (two-way)
    firestoreDb.collection("users")
        .document(myEmail)
        .collection("shared_locations")
        .get()
        .addOnSuccessListener { sharedLocSnapshot ->
            for (sharedLocDoc in sharedLocSnapshot) {
                val receiverEmail = sharedLocDoc.id
                // If the receiver has accepted (status == active), update my shared_locations in their view
                val status = sharedLocDoc.getString("status")
                if (status == "active") {
                    updateSharedLocation(receiverEmail, myEmail)
                }
            }
        }
}

fun startAutoWorkingStatusListeners() {
    val firestoreDb = Firebase.firestore
    val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
    firestoreDb.collection("users")
        .document(myEmail)
        .collection("invites")
        .whereEqualTo("invite_status", 1)
        .get()
        .addOnSuccessListener { invitesSnapshot ->
            for (inviteDoc in invitesSnapshot) {
                val senderEmail = inviteDoc.id
                setupWorkingStatusListener(myEmail, senderEmail)
            }
        }
}

/**
 * Periodically checks all shared_locations for the current user (as sender) and sets online_status to false
 * if the last timestamp is older than 10 seconds.
 */
fun startOnlineStatusMonitor() {
    val firestoreDb = Firebase.firestore
    val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            kotlinx.coroutines.delay(10000)
            // For each shared_locations where this user is the sender
            firestoreDb.collection("users")
                .document(myEmail)
                .collection("shared_locations")
                .get()
                .addOnSuccessListener { snapshot ->
                    val now = System.currentTimeMillis()
                    for (doc in snapshot.documents) {
                        val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                        val isOnline = (now - timestamp) <= 10000
                        // Only set to false if currently true and stale
                        if (!isOnline && (doc.getBoolean("online_status") == true)) {
                            firestoreDb.collection("users")
                                .document(myEmail)
                                .collection("shared_locations")
                                .document(doc.id)
                                .update("online_status", false)
                        }
                    }
                }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchPreview() {
    Trackmate1Theme {
        SearchScreen()
    }
}
