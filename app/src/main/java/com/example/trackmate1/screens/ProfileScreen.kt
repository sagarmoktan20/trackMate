package com.example.trackmate1.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.trackmate1.NavigationItems
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val imageUrl: String = "",
    val workingStatus: String = "Free"
)

// Data class for follower
data class FollowerData(val email: String, val name: String, val imageUrl: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var isLoading by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    val workingStatusOptions = listOf("Free", "Busy", "En-route")

    // Following count state
    var followingCount by remember { mutableStateOf(0) }
    // Follower count state
    var followerCount by remember { mutableStateOf(0) }

    // Following dialog state
    var showFollowingDialog by remember { mutableStateOf(false) }
    var followingList by remember { mutableStateOf(listOf<UserProfile>()) }
    // Follower dialog state (for future use)
    var showFollowerDialog by remember { mutableStateOf(false) }
    var followerList by remember { mutableStateOf(listOf<FollowerData>()) }

    // Add state for phone dialog
    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }

    // Listen to shared_locations collection for following count
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Firebase.firestore.collection("users")
                .document(currentUser.email.toString())
                .collection("shared_locations")
                .addSnapshotListener { snapshot, _ ->
                    followingCount = snapshot?.size() ?: 0
                }
            // Listen to invites collection for follower count
            Firebase.firestore.collection("users")
                .document(currentUser.email.toString())
                .collection("invites")
                .whereEqualTo("invite_status", 1)
                .addSnapshotListener { snapshot, _ ->
                    followerCount = snapshot?.size() ?: 0
                }
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Firebase.firestore.collection("users")
                .document(currentUser.email.toString())
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userProfile = UserProfile(
                            name = document.getString("Name") ?: "",
                            email = document.getString("email") ?: "",
                            phone = document.getString("phone") ?: "",
                            imageUrl = document.getString("imageUrl") ?: "",
                            workingStatus = document.getString("workingStatus") ?: "Free"
                        )
                    } else {
                        // If document doesn't exist, create it with default values
                        val defaultProfile = UserProfile(
                            name = currentUser.displayName ?: "",
                            email = currentUser.email ?: "",
                            workingStatus = "Free"
                        )
                        Firebase.firestore.collection("users")
                            .document(currentUser.email.toString())
                            .set(defaultProfile)
                            .addOnSuccessListener {
                                userProfile = defaultProfile
                                isLoading = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error creating profile", Toast.LENGTH_SHORT).show()
                                isLoading = false
                            }
                        return@addOnSuccessListener
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
        }
    }

    // Fetch following list when dialog is shown
    LaunchedEffect(showFollowingDialog) {
        if (showFollowingDialog) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Firebase.firestore.collection("users")
                    .document(currentUser.email.toString())
                    .collection("shared_locations")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val emails = snapshot.documents.map { it.id }
                        if (emails.isNotEmpty()) {
                            Firebase.firestore.collection("users")
                                .whereIn("email", emails.take(10)) // Firestore whereIn max 10
                                .get()
                                .addOnSuccessListener { userDocs ->
                                    followingList = userDocs.documents.map { doc ->
                                        UserProfile(
                                            name = doc.getString("Name") ?: "",
                                            email = doc.getString("email") ?: "",
                                            imageUrl = doc.getString("imageUrl") ?: "",
                                            workingStatus = doc.getString("workingStatus") ?: "Free"
                                        )
                                    }
                                }
                        } else {
                            followingList = emptyList()
                        }
                    }
            }
        }
    }

    // Fetch follower list when dialog is shown
    LaunchedEffect(showFollowerDialog) {
        if (showFollowerDialog) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Firebase.firestore.collection("users")
                    .document(currentUser.email.toString())
                    .collection("invites")
                    .whereEqualTo("invite_status", 1)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        followerList = snapshot.documents.map { doc ->
                            FollowerData(
                                email = doc.id,
                                name = doc.getString("sender_name") ?: doc.id,
                                imageUrl = doc.getString("sender_imageUrl") ?: ""
                            )
                        }
                    }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Email at top left
            Text(
                text = userProfile.email,
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
            // Instagram-like top row: Profile Pic + Follower/Following
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture (top left)
                if (userProfile.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(userProfile.imageUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = Color.LightGray,
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = Color.LightGray,
                                shape = CircleShape
                            )
                    )
                }
                // Follower and Following counts (right of profile pic)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = followerCount.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue,
                            modifier = Modifier.clickable { showFollowerDialog = true }
                        )
                        Text(
                            text = "Followers",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = followingCount.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue,
                            modifier = Modifier.clickable { showFollowingDialog = true }
                        )
                        Text(
                            text = "Following",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }
                }
            }
            // Name and phone number below profile pic, left-aligned
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.width(120.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // User Name
                    Text(
                        text = userProfile.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 4.dp, bottom = 2.dp)
                            .fillMaxWidth()
                    )
                    // Phone Number (if available)
                    val phoneValue = userProfile.phone
                    if (!phoneValue.isNullOrEmpty() && phoneValue != "null") {
                        Text(
                            text = phoneValue,
                            fontSize = 16.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    } else {
                        Button(onClick = { showPhoneDialog = true }, modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
                            Text("Add PhoneNum")
                        }
                    }
                }
            }
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                // Working Status Dropdown
                Text(
                    text = "Working Status:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = userProfile.workingStatus,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        workingStatusOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser != null) {
                                        // Update local state
                                        userProfile = userProfile.copy(workingStatus = status)
                                        
                                        // Update Firebase
                                        Firebase.firestore.collection("users")
                                            .document(currentUser.email.toString())
                                            .update("workingStatus", status)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Status updated to $status", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Sign Out Button
                Button(
                    onClick = {
                        signOut(
                            context,
                            webClientId = "9964737077-gp26ao3ddcl27dni79sd6ugkkf1hpn2p.apps.googleusercontent.com"
                        ) {
                            Toast.makeText(context, "Signed Out", Toast.LENGTH_SHORT).show()
                            navController.navigate(NavigationItems.Login.route) {
                                popUpTo(0)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Sign Out", color = Color.White, fontSize = 20.sp)
                }
            }

            // Phone input dialog
            if (showPhoneDialog) {
                AlertDialog(
                    onDismissRequest = { showPhoneDialog = false },
                    title = { Text("Add Phone Number") },
                    text = {
                        TextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number") }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null && phoneInput.isNotBlank()) {
                                Firebase.firestore.collection("users")
                                    .document(currentUser.email.toString())
                                    .update("phone", phoneInput)
                                    .addOnSuccessListener {
                                        userProfile = userProfile.copy(phone = phoneInput)
                                        showPhoneDialog = false
                                        phoneInput = ""
                                    }
                            }
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPhoneDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Following dialog
            if (showFollowingDialog) {
                AlertDialog(
                    onDismissRequest = { showFollowingDialog = false },
                    title = { Text("Following") },
                    text = {
                        if (followingList.isEmpty()) {
                            Text("You are not following anyone.")
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(followingList) { user ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        if (user.imageUrl.isNotEmpty()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(user.imageUrl),
                                                contentDescription = "Profile Image",
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = user.name, fontWeight = FontWeight.Bold)
                                            Text(text = user.email, fontSize = 12.sp, color = Color.Gray)
                                        }
                                        Button(onClick = {
                                            // Unfollow: remove this user from shared_locations
                                            val currentUser = FirebaseAuth.getInstance().currentUser
                                            if (currentUser != null) {
                                                // 1. Remove from my shared_locations
                                                Firebase.firestore.collection("users")
                                                    .document(currentUser.email.toString())
                                                    .collection("shared_locations")
                                                    .document(user.email)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        // Optionally update UI or show a message
                                                    }
                                                // 2. Remove myself from their invites (invite_status == 1)
                                                Firebase.firestore.collection("users")
                                                    .document(user.email)
                                                    .collection("invites")
                                                    .document(currentUser.email.toString())
                                                    .get()
                                                    .addOnSuccessListener { docSnap ->
                                                        if (docSnap.exists() && (docSnap.getLong("invite_status") == 1L)) {
                                                            docSnap.reference.delete()
                                                        }
                                                    }
                                            }
                                        }) {
                                            Text("Unfollow")
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFollowingDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Follower dialog
            if (showFollowerDialog) {
                AlertDialog(
                    onDismissRequest = { showFollowerDialog = false },
                    title = { Text("Followers") },
                    text = {
                        if (followerList.isEmpty()) {
                            Text("You have no followers.")
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(followerList) { follower ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        if (follower.imageUrl.isNotEmpty()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(follower.imageUrl),
                                                contentDescription = "Profile Image",
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = follower.name, fontWeight = FontWeight.Bold)
                                            Text(text = follower.email, fontSize = 12.sp, color = Color.Gray)
                                        }
                                        Button(onClick = {
                                            // Remove follower: remove this sender from our invites and from their shared_locations
                                            val currentUser = FirebaseAuth.getInstance().currentUser
                                            if (currentUser != null) {
                                                // 1. Remove from my invites
                                                Firebase.firestore.collection("users")
                                                    .document(currentUser.email.toString())
                                                    .collection("invites")
                                                    .document(follower.email)
                                                    .delete()
                                                // 2. Remove myself from their shared_locations
                                                Firebase.firestore.collection("users")
                                                    .document(follower.email)
                                                    .collection("shared_locations")
                                                    .document(currentUser.email.toString())
                                                    .delete()
                                            }
                                        }) {
                                            Text("Remove")
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFollowerDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

fun signOut(context: Context, webClientId: String, onComplete: () -> Unit) {
    Firebase.auth.signOut()
    val googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOption)
    googleSignInClient.signOut().addOnCompleteListener {
        val prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("login_status", false).apply()
        onComplete()
    }
}

//@Preview
//@Composable
//fun ProfilePreview(){
//   Trackmate1Theme  {
//        ProfileScreen()
//    }
//}