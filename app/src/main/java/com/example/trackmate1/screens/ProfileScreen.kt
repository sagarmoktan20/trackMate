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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.ColumnScopeInstance.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import com.example.trackmate1.ui.theme.Trackmate1Theme


data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val imageUrl: String = "",
    val workingStatus: String = "Free",
    val isAdmin: Boolean = false
)

// Data class for follower
data class FollowerData(val email: String, val name: String, val imageUrl: String)

// Data class for assigned task
data class AssignedTask(
    val taskId: String,
    val clientName: String,
    val status: String
)

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
    // Tasks count state
    var tasksCount by remember { mutableStateOf(0) }

    // Following dialog state
    var showFollowingDialog by remember { mutableStateOf(false) }
    var followingList by remember { mutableStateOf(listOf<UserProfile>()) }
    // Follower dialog state (for future use)
    var showFollowerDialog by remember { mutableStateOf(false) }
    var followerList by remember { mutableStateOf(listOf<FollowerData>()) }
    // Tasks dialog state (for future use)
    var showTasksDialog by remember { mutableStateOf(false) }
    var tasksList by remember { mutableStateOf(listOf<AssignedTask>()) }

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
            // Listen to assigned_tasks collection for tasks count
            Firebase.firestore.collection("users")
                .document(currentUser.email.toString())
                .collection("assigned_tasks")
                .addSnapshotListener { snapshot, _ ->
                    tasksCount = snapshot?.size() ?: 0
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
                            workingStatus = document.getString("workingStatus") ?: "Free",
                            isAdmin = document.getBoolean("isAdmin") ?: false
                        )
                    } else {
                        // If document doesn't exist, create it with default values
                        val defaultProfile = UserProfile(
                            name = currentUser.displayName ?: "",
                            email = currentUser.email ?: "",
                            workingStatus = "Free",
                            isAdmin = false
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

    // Fetch tasks list when dialog is shown
    LaunchedEffect(showTasksDialog) {
        if (showTasksDialog) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Firebase.firestore.collection("users")
                    .document(currentUser.email.toString())
                    .collection("assigned_tasks")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        tasksList = snapshot.documents.map { doc ->
                            AssignedTask(
                                taskId = doc.id,
                                clientName = doc.getString("clientName") ?: "Unknown Client",
                                status = doc.getString("status") ?: "pending"
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileScreen", "Error fetching tasks: ${e.message}")
                    }
            }
        }
    }

    // State for settings popup dialog
    var showSettingsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top row with email and settings icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp, start = 1.dp), // push away from screen edge
                horizontalArrangement = Arrangement.End, // align to left
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.Gray
                    )
                }
            }


            // Profile Picture (centered)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (userProfile.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(userProfile.imageUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
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
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = Color.LightGray,
                                shape = CircleShape
                            )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (userProfile.isAdmin) {
                    Text(
                        text = "Administrator",
                        fontSize = 14.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }else{
                    Text(
                        text = "Service technician",
                        fontSize = 14.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // User Name (centered below profile pic)
                Text(
                    text = userProfile.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Follower, Following, and Tasks counts (below profile pic)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Followers Card
                    StatCard(
                        label = "Followers",
                        count = followerCount,
                        color = Color.Blue
                    ) { showFollowerDialog = true }

                    // Following Card
                    StatCard(
                        label = "Following",
                        count = followingCount,
                        color = Color.Blue
                    ) { showFollowingDialog = true }

                    // Tasks Card
                    StatCard(
                        label = "Tasks",
                        count = tasksCount,
                        color = Color.Green
                    ) { showTasksDialog = true }
                }



            }


            Spacer(modifier = Modifier.height(0.dp))

            Text(
                text = "About",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Profile information in key-value table format
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Divider(color = Color.LightGray, thickness = 1.dp)

                // Email row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Key (Email)
                    Text(
                        text = "Email:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(100.dp)
                    )
                    Log.d("ProfileScreen", "Email: ${userProfile.email}")

                    // Value (Email address)
                    Text(
                        text = userProfile.email,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
                Divider(color = Color.LightGray, thickness = 1.dp)

                // Phone number row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Key (Phone)
                    Text(
                        text = "Phone:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(100.dp)
                    )

                    // Value (Phone number or Add button)
                    val phoneValue = userProfile.phone
                    if (!phoneValue.isNullOrEmpty() && phoneValue != "null") {
                        Text(
                            text = phoneValue,
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    } else {
                        Button(onClick = { showPhoneDialog = true }) {
                            Text("Add PhoneNum")
                        }
                    }
                    //Key(work status)


                }

                Divider(color = Color.LightGray, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Key (Email)
                    Text(
                        text = "Work status:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(100.dp)
                    )

                    // Value (drop down menu of work status)


            if (isLoading) {
                CircularProgressIndicator()
            } else {
                // Working Status Dropdown
//                Text(
//                    text = "Working Status:",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium,
//                    color = Color.Black
//                )

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
                            .menuAnchor().width(120.dp).height(50.dp).size(6.dp),
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .width(160.dp)        // shrink width
                            .heightIn(max = 150.dp) // shrink height if too many items
                    ) {
                        workingStatusOptions.forEach { status ->
                            DropdownMenuItem(
                                modifier = Modifier.height(36.dp), // smaller row height
                                text = {
                                    Text(
                                        text = status,
                                        fontSize = 14.sp // smaller text
                                    )
                                },
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
                                                Toast.makeText(
                                                    context,
                                                    "Status updated to $status",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to update status",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }


                  //  Spacer(modifier = Modifier.weight(1f))
            }
                }
                Divider(color = Color.LightGray, thickness = 1.dp)

            }
                Divider(color = Color.LightGray, thickness = 1.dp)

            }
                // Sign Out Button
//                Button(
//                    onClick = {
//                        signOut(
//                            context,
//                            webClientId = "9964737077-gp26ao3ddcl27dni79sd6ugkkf1hpn2p.apps.googleusercontent.com"
//                        ) {
//                            Toast.makeText(context, "Signed Out", Toast.LENGTH_SHORT).show()
//                            navController.navigate(NavigationItems.Login.route) {
//                                popUpTo(0)
//                            }
//                        }
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text(text = "Sign Out", color = Color.White, fontSize = 20.sp)
//                }


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

            // Tasks dialog
            if (showTasksDialog) {
                AlertDialog(
                    onDismissRequest = { showTasksDialog = false },
                    title = { Text("Assigned Tasks") },
                    text = {
                        if (tasksList.isEmpty()) {
                            Text("You have no assigned tasks.")
                        } else {
                            val hasAcceptedTask = tasksList.any { it.status == "accepted" }
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(tasksList) { task ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 2.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        border = BorderStroke(1.dp, Color.LightGray)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            // Client name
                                            Text(
                                                text = task.clientName,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Black
                                            )

                                            // Accept/Reject or In Progress
                                            if (task.status == "pending") {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = {
                                                            val currentUser = FirebaseAuth.getInstance().currentUser?.email
                                                            if (currentUser != null) {
                                                                Firebase.firestore.collection("users")
                                                                    .document(currentUser)
                                                                    .collection("assigned_tasks")
                                                                    .document(task.taskId)
                                                                    .get()
                                                                    .addOnSuccessListener { taskDoc ->
                                                                        if (taskDoc.exists()) {
                                                                            val clientLat = taskDoc.getDouble("clientLat")
                                                                            val clientLng = taskDoc.getDouble("clientLng")
                                                                            val clientName = taskDoc.getString("clientName") ?: ""
                                                                            val clientPhone = taskDoc.getString("clientPhone") ?: ""

                                                                            if (clientLat != null && clientLng != null) {
                                                                                Firebase.firestore.collection("users")
                                                                                    .document(currentUser)
                                                                                    .collection("assigned_tasks")
                                                                                    .document(task.taskId)
                                                                                    .update("status", "accepted")
                                                                                    .addOnSuccessListener {
                                                                                        Toast.makeText(
                                                                                            context,
                                                                                            "Task accepted! Client location will be shown on map.",
                                                                                            Toast.LENGTH_SHORT
                                                                                        ).show()
                                                                                        showTasksDialog = false
                                                                                        val prefs = context.getSharedPreferences(
                                                                                            "task_prefs",
                                                                                            Context.MODE_PRIVATE
                                                                                        )
                                                                                        prefs.edit().apply {
                                                                                            putString("accepted_task_id", task.taskId)
                                                                                            putString("accepted_client_name", clientName)
                                                                                            putString("accepted_client_phone", clientPhone)
                                                                                            putFloat("accepted_client_lat", clientLat.toFloat())
                                                                                            putFloat("accepted_client_lng", clientLng.toFloat())
                                                                                            putBoolean("has_accepted_task", true)
                                                                                        }.apply()
                                                                                        tasksList = tasksList.map {
                                                                                            if (it.taskId == task.taskId)
                                                                                                it.copy(status = "accepted")
                                                                                            else it
                                                                                        }
                                                                                    }
                                                                                    .addOnFailureListener { e ->
                                                                                        Toast.makeText(
                                                                                            context,
                                                                                            "Failed to accept task: ${e.message}",
                                                                                            Toast.LENGTH_SHORT
                                                                                        ).show()
                                                                                    }
                                                                            } else {
                                                                                Toast.makeText(context, "Error: Invalid client coordinates", Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        } else {
                                                                            Toast.makeText(context, "Error: Task not found", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                    .addOnFailureListener { e ->
                                                                        Toast.makeText(context, "Error fetching task details: ${e.message}", Toast.LENGTH_SHORT).show()
                                                                    }
                                                            }


                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFFA8E6CF), // Pastel green
                                                            contentColor = Color(0xFF2E7D32)    // Darker green text
                                                        ),
                                                        modifier = Modifier
                                                            .width(70.dp)
                                                            .height(35.dp)
                                                            .border(1.dp, Color(0xFF81C784), RectangleShape), // Subtle green border
                                                        shape = RectangleShape,
                                                        enabled = !hasAcceptedTask // Disable if any task is already accepted
                                                    ) {
                                                        Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    if (hasAcceptedTask) {
                                                        Text(
                                                            "Finish your current task to accept another",
                                                            color = Color.Red,
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.padding(start = 4.dp)
                                                        )
                                                    }
                                                    Button(
                                                        onClick = {
                                                            // TODO: Handle reject
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFFFF8C94), // Pastel red
                                                            contentColor = Color(0xFFC62828)    // Darker red text
                                                        ),
                                                        modifier = Modifier
                                                            .width(70.dp)
                                                            .height(35.dp)
                                                            .border(1.dp, Color(0xFFE57373), RectangleShape), // Subtle red border
                                                        shape = RectangleShape,
                                                        enabled = !hasAcceptedTask
                                                    ) {
                                                        Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            } else if (task.status == "accepted") {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = Color(0xFF43A047), // Green
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        "In Progress",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTasksDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Settings dialog
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Settings") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    showSettingsDialog = false
                                    showPhoneDialog = true

                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                            ) {
                                Text("Change Phone Number", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    showSettingsDialog = false
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
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Sign Out", color = Color.White)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    count: Int,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            //.weight(1f) // equal space for each
            .padding(6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color.LightGray),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
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
//        ProfileScreen(
//            navController = TODO()
//        )
//    }
//}