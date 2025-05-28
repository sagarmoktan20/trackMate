package com.example.trackmate1.screens

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackmate1.ui.theme.Trackmate1Theme
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ListenerRegistration


@Composable
fun SearchScreen() {
    var searchText by remember { mutableStateOf("") }
    val invitationList = remember { mutableStateListOf<String>() } // placeholder list

    LaunchedEffect(Unit) {
        getInvites(invitationList)
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
                InvitationItem(email = email)
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
fun InvitationItem(email: String) {
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
                    val firestoreDb = Firebase.firestore;
                    firestoreDb.collection("users")
                        .document(FirebaseAuth.getInstance().currentUser?.email.toString())
                        .collection("invites").document(email).update("invite_status",1)

                },
                modifier = Modifier.width(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                Text("Accept")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Reject Button
            Button(
                onClick = { val firestoreDb = Firebase.firestore;
                    firestoreDb.collection("users")
                        .document(FirebaseAuth.getInstance().currentUser?.email.toString())
                        .collection("invites").document(email).update("invite_status",-1) },
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

@Preview(showBackground = true)
@Composable
fun SearchPreview() {
    Trackmate1Theme {
        SearchScreen()
    }
}
