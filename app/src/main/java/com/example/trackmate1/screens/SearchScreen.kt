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


@Composable
fun SearchScreen() {
    var searchText by remember { mutableStateOf("") }
    val invitationList = remember { mutableStateListOf<String>() } // placeholder list
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        getInvites(invitationList)
        startAutoLocationSharing(context)
        startAutoWorkingStatusListeners()
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
                            // Get current working status
                            firestoreDb.collection("users")
                                .document(myEmail)
                                .get()
                                .addOnSuccessListener { userDocument ->
                                    val currentWorkingStatus = userDocument.getString("workingStatus") ?: "Free"
                                    
                                    // After successful status update, create shared_locations document in sender's collection
                                    val sharedLocationData = hashMapOf(
                                        "status" to "active",
                                        "shared_by" to myEmail,
                                        "working_status" to currentWorkingStatus,
                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                    
                                    // Create the shared_locations document in the sender's collection
                                    firestoreDb.collection("users")
                                        .document(email)  // This is the sender's document
                                        .collection("shared_locations")
                                        .document(myEmail)  // This is the receiver's email
                                        .set(sharedLocationData)
                                        .addOnSuccessListener {
                                            Log.d("Firestore", "Successfully created shared_locations document with working status")
                                            
                                            // Set up global working status listener for this user
                                            setupWorkingStatusListener(myEmail, email)
                                            
                                            // Now start sharing location updates
                                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                            
                                            try {
                                                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                                                    .setWaitForAccurateLocation(false)
                                                    .setMinUpdateIntervalMillis(5000)
                                                    .setMaxUpdateDelayMillis(10000)
                                                    .build()

                                                val locationCallback = object : LocationCallback() {
                                                    override fun onLocationResult(locationResult: LocationResult) {
                                                        locationResult.lastLocation?.let { location ->
                                                            // Update the shared_locations document with new coordinates
                                                            firestoreDb.collection("users")
                                                                .document(email)  // Sender's document
                                                                .collection("shared_locations")
                                                                .document(myEmail)  // Receiver's email
                                                                .update(
                                                                    mapOf(
                                                                        "latitude" to location.latitude,
                                                                        "longitude" to location.longitude,
                                                                        "timestamp" to FieldValue.serverTimestamp()
                                                                    )
                                                                )
                                                        }
                                                    }
                                                }

                                                fusedLocationClient.requestLocationUpdates(
                                                    locationRequest,
                                                    locationCallback,
                                                    null
                                                )
                                            } catch (e: SecurityException) {
                                                Log.e("SearchScreen", "Error requesting location updates", e)
                                            }
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

fun startAutoLocationSharing(context: Context) {
    val firestoreDb = Firebase.firestore
    val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    firestoreDb.collection("users")
        .document(myEmail)
        .collection("invites")
        .whereEqualTo("invite_status", 1)
        .get()
        .addOnSuccessListener { invitesSnapshot ->
            for (inviteDoc in invitesSnapshot) {
                val senderEmail = inviteDoc.id
                // Check if shared_locations doc exists and is active
                firestoreDb.collection("users")
                    .document(senderEmail)
                    .collection("shared_locations")
                    .document(myEmail)
                    .get()
                    .addOnSuccessListener { sharedLocDoc ->
                        if (sharedLocDoc.exists() && sharedLocDoc.getString("status") == "active") {
                            // Start location sharing for this sender
                            try {
                                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                                    .setWaitForAccurateLocation(false)
                                    .setMinUpdateIntervalMillis(5000)
                                    .setMaxUpdateDelayMillis(10000)
                                    .build()

                                val locationCallback = object : LocationCallback() {
                                    override fun onLocationResult(locationResult: LocationResult) {
                                        locationResult.lastLocation?.let { location ->
                                            firestoreDb.collection("users")
                                                .document(senderEmail)
                                                .collection("shared_locations")
                                                .document(myEmail)
                                                .update(
                                                    mapOf(
                                                        "latitude" to location.latitude,
                                                        "longitude" to location.longitude,
                                                        "timestamp" to FieldValue.serverTimestamp()
                                                    )
                                                )
                                        }
                                    }
                                }
                                fusedLocationClient.requestLocationUpdates(
                                    locationRequest,
                                    locationCallback,
                                    null
                                )
                            } catch (e: SecurityException) {
                                Log.e("AutoLocationShare", "Error requesting location updates", e)
                            }
                        }
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

@Preview(showBackground = true)
@Composable
fun SearchPreview() {
    Trackmate1Theme {
        SearchScreen()
    }
}
