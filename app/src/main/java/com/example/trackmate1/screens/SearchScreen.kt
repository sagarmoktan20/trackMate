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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
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

// Data class for invitation
data class InvitationData(val email: String, val imageUrl: String)

@Composable
fun SearchScreen() {
    var searchText by remember { mutableStateOf("") }
    val invitationList = remember { mutableStateListOf<InvitationData>() } // now holds email and imageUrl
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        getInvites(invitationList)
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
                // Email validation logic
                val trimmedEmail = searchText.trim()
                if (trimmedEmail.isBlank()) {
                    android.widget.Toast.makeText(context, "Please enter an email address.", android.widget.Toast.LENGTH_SHORT).show()
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                    android.widget.Toast.makeText(context, "Please enter a valid email address.", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    sendInvite(trimmedEmail)
                }
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
            items(invitationList) { invitation ->
                InvitationItem(email = invitation.email, imageUrl = invitation.imageUrl, context = context)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
var inviteListener: ListenerRegistration? = null

fun getInvites(invitationList: SnapshotStateList<InvitationData>) {
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
                        val imageUrl = item.getString("sender_imageUrl") ?: ""
                        invitationList.add(InvitationData(item.id, imageUrl))
                    }
                }
            }
        }
}

fun sendInvite(searchText: String) {
    val mailFromSearchBar = searchText
    val firestoreDb = Firebase.firestore
    val myMail = FirebaseAuth.getInstance().currentUser?.email.toString()
    // Fetch sender's profile imageUrl and name from Firestore
    firestoreDb.collection("users")
        .document(myMail)
        .get()
        .addOnSuccessListener { document ->
            val senderImageUrl = document.getString("imageUrl") ?: ""
            val senderName = document.getString("Name") ?: ""
            val data = hashMapOf(
                "invite_status" to 0,
                "sender_imageUrl" to senderImageUrl,
                "sender_name" to senderName
            )
            firestoreDb.collection("users")
                .document(mailFromSearchBar).collection("invites")
                .document(myMail).set(data)
                .addOnSuccessListener {}
                .addOnFailureListener {}
        }
}

@Composable
fun InvitationItem(email: String, imageUrl: String, context: Context) {
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
            // Image (left, circular, larger)
            if (imageUrl.isNotEmpty()) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(imageUrl),
                    contentDescription = "Sender Image",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .padding(end = 8.dp)
                )
            }
            // Email (right of image)
            Text(
                text = email,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            // Accept Button (right)
            Button(
                onClick = {
                    val firestoreDb = Firebase.firestore
                    val myEmail = FirebaseAuth.getInstance().currentUser?.email.toString()
                    
                    // First, get the current user's data immediately for shared_locations creation
                    firestoreDb.collection("users")
                        .document(myEmail)
                        .get()
                        .addOnSuccessListener { userDocument ->
                            val currentWorkingStatus = userDocument.getString("workingStatus") ?: "Free"
                            val receiverName = userDocument.getString("Name") ?: ""
                            val receiverImageUrl = userDocument.getString("imageUrl") ?: ""
                            val receiverPhone = userDocument.getString("phone")
                            
                            // Create shared_locations document data
                            val sharedLocationData = hashMapOf(
                                "status" to "active",
                                "shared_by" to myEmail,
                                "working_status" to currentWorkingStatus,
                                "receiver_name" to receiverName,
                                "receiver_imageUrl" to receiverImageUrl,
                                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                "online_status" to true
                            )
                            if (!receiverPhone.isNullOrBlank() && receiverPhone != "null") {
                                sharedLocationData["receiver_phone"] = receiverPhone
                            }
                            
                            // Create shared_locations document immediately in sender's collection
                            firestoreDb.collection("users")
                                .document(email)  // This is the sender's document
                                .collection("shared_locations")
                                .document(myEmail)  // This is the receiver's email
                                .set(sharedLocationData)
                                .addOnSuccessListener {
                                    android.util.Log.d("Firestore", "Successfully created shared_locations document with working status, name, and imageUrl")
                                    // Set up global working status listener for this user
                                    setupWorkingStatusListener(myEmail, email)
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("Firestore", "Error creating shared_locations document", e)
                                }
                            
                            // Now update the invite status
                            firestoreDb.collection("users")
                                .document(myEmail)
                                .collection("invites")
                                .document(email)
                                .update("invite_status", 1)
                                .addOnSuccessListener {
                                    // Check if sender is admin and handle admin-specific logic
                                    firestoreDb.collection("users")
                                        .document(email)
                                        .get()
                                        .addOnSuccessListener { senderDoc ->
                                            val isAdmin = senderDoc.getBoolean("isAdmin") ?: false
                                            if (isAdmin) {
                                                // Fetch all emails in sender's shared_locations
                                                firestoreDb.collection("users")
                                                    .document(email)
                                                    .collection("shared_locations")
                                                    .get()
                                                    .addOnSuccessListener { followingSnapshot ->
                                                        val followedEmails = followingSnapshot.documents.map { it.id }.filter { it != myEmail }
                                                        android.util.Log.d("AdminAutoConnect", "Admin is following: $followedEmails")
                                                        
                                                        // For each followed email, add the accepting user to their shared_locations
                                                        followedEmails.forEach { followedEmail ->
                                                            // Fetch the details of the current user (myEmail)
                                                            firestoreDb.collection("users")
                                                                .document(myEmail)
                                                                .get()
                                                                .addOnSuccessListener { currentUserDoc ->
                                                                    val workingStatus = currentUserDoc.getString("workingStatus") ?: "Free"
                                                                    val receiverName = currentUserDoc.getString("Name") ?: ""
                                                                    val receiverImageUrl = currentUserDoc.getString("imageUrl") ?: ""
                                                                    val receiverPhone = currentUserDoc.getString("phone")
                                                                    val sharedLocationData = hashMapOf(
                                                                        "status" to "active",
                                                                        "shared_by" to email, // admin
                                                                        "connected_user" to myEmail,
                                                                        "working_status" to workingStatus,
                                                                        "receiver_name" to receiverName,
                                                                        "receiver_imageUrl" to receiverImageUrl,
                                                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                                                        "online_status" to true
                                                                    )
                                                                    if (!receiverPhone.isNullOrBlank() && receiverPhone != "null") {
                                                                        sharedLocationData["receiver_phone"] = receiverPhone
                                                                    }
                                                                    firestoreDb.collection("users")
                                                                        .document(followedEmail)
                                                                        .collection("shared_locations")
                                                                        .document(myEmail)
                                                                        .set(sharedLocationData)
                                                                    // Also add an invite for the accepting user in the followed user's invites with invite_status = 1
                                                                    val inviteData = hashMapOf(
                                                                        "invite_status" to 1,
                                                                        "sender_imageUrl" to "",
                                                                        "sender_name" to ""
                                                                    )
                                                                    firestoreDb.collection("users")
                                                                        .document(myEmail)
                                                                        .collection("invites")
                                                                        .document(followedEmail)
                                                                        .set(inviteData)
                                                                    // Set up working status listener for the current user with this followed user
                                                                    setupWorkingStatusListener(myEmail, followedEmail)
                                                                }
                                                        }
                                                        
                                                        // NEW CODE: Add all existing users to the accepting user's shared_locations
                                                        // This makes the accepting user (E) also see all existing users (A, B, C, D)
                                                        followedEmails.forEach { existingUserEmail ->
                                                            // Fetch the details of each existing user
                                                            firestoreDb.collection("users")
                                                                .document(existingUserEmail)
                                                                .get()
                                                                .addOnSuccessListener { existingUserDoc ->
                                                                    val existingUserWorkingStatus = existingUserDoc.getString("workingStatus") ?: "Free"
                                                                    val existingUserName = existingUserDoc.getString("Name") ?: ""
                                                                    val existingUserImageUrl = existingUserDoc.getString("imageUrl") ?: ""
                                                                    val existingUserPhone = existingUserDoc.getString("phone")
                                                                    
                                                                    val existingUserSharedLocationData = hashMapOf(
                                                                        "status" to "active",
                                                                        "shared_by" to existingUserEmail,
                                                                        "working_status" to existingUserWorkingStatus,
                                                                        "receiver_name" to existingUserName,
                                                                        "receiver_imageUrl" to existingUserImageUrl,
                                                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                                                        "online_status" to true
                                                                    )
                                                                    if (!existingUserPhone.isNullOrBlank() && existingUserPhone != "null") {
                                                                        existingUserSharedLocationData["receiver_phone"] = existingUserPhone
                                                                    }
                                                                    
                                                                    // Add this existing user to the accepting user's shared_locations
                                                                    firestoreDb.collection("users")
                                                                        .document(myEmail)  // Accepting user's document
                                                                        .collection("shared_locations")
                                                                        .document(existingUserEmail)  // Existing user's email
                                                                        .set(existingUserSharedLocationData)
                                                                        .addOnSuccessListener {
                                                                            android.util.Log.d("AdminAutoConnect", "Added existing user $existingUserEmail to accepting user $myEmail shared_locations")
                                                                            // Set up working status listener for the existing user with the accepting user
                                                                            setupWorkingStatusListener(existingUserEmail, myEmail)
                                                                        }
                                                                        .addOnFailureListener { e ->
                                                                            android.util.Log.e("AdminAutoConnect", "Error adding existing user $existingUserEmail to accepting user $myEmail shared_locations", e)
                                                                        }
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    android.util.Log.e("AdminAutoConnect", "Error fetching existing user $existingUserEmail details", e)
                                                                }
                                                        }
                                                        
                                                        // NEW CODE: Add the accepting user (E) to the invites collection of all existing users (A, B, C, D)
                                                        // This makes E appear in their "Following" list in ProfileScreen
                                                        followedEmails.forEach { existingUserEmail ->
                                                            // Fetch the accepting user's (E) details for sender information
                                                            firestoreDb.collection("users")
                                                                .document(myEmail)
                                                                .get()
                                                                .addOnSuccessListener { acceptingUserDoc ->
                                                                    val acceptingUserName = acceptingUserDoc.getString("Name") ?: ""
                                                                    val acceptingUserImageUrl = acceptingUserDoc.getString("imageUrl") ?: ""
                                                                    
                                                                    // Debug logging to check the values
                                                                    android.util.Log.d("AdminAutoConnect", "Accepting user $myEmail details - Name: '$acceptingUserName', ImageUrl: '$acceptingUserImageUrl'")
                                                                    
                                                                    val inviteData = hashMapOf(
                                                                        "invite_status" to 1,
                                                                        "sender_imageUrl" to acceptingUserImageUrl,
                                                                        "sender_name" to acceptingUserName
                                                                    )
                                                                    
                                                                    // Debug logging to check the invite data
                                                                    android.util.Log.d("AdminAutoConnect", "Invite data for $existingUserEmail: $inviteData")
                                                                    
                                                                    // Add E to the existing user's invites collection
                                                                    firestoreDb.collection("users")
                                                                        .document(existingUserEmail)  // Existing user's document
                                                                        .collection("invites")
                                                                        .document(myEmail)  // Accepting user's email
                                                                        .set(inviteData)
                                                                        .addOnSuccessListener {
                                                                            android.util.Log.d("AdminAutoConnect", "Added accepting user $myEmail to existing user $existingUserEmail invites with status 1, name: '$acceptingUserName', imageUrl: '$acceptingUserImageUrl'")
                                                                        }
                                                                        .addOnFailureListener { e ->
                                                                            android.util.Log.e("AdminAutoConnect", "Error adding accepting user $myEmail to existing user $existingUserEmail invites", e)
                                                                        }
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    android.util.Log.e("AdminAutoConnect", "Error fetching accepting user $myEmail details for invites", e)
                                                                }
                                                        }
                                                    }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            android.util.Log.e("Firestore", "Error checking admin status", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("Firestore", "Error updating invite status", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("Firestore", "Error getting user document for shared_locations creation", e)
                        }
                },
                modifier = Modifier.width(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                Text("Accept", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Reject Button (right)
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
                Text("Reject", fontSize = 12.sp)
            }
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