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

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val imageUrl: String = "",
    val workingStatus: String = "Free"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var isLoading by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    val workingStatusOptions = listOf("Free", "Busy", "En-route")


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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                // Profile Picture
                if (userProfile.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(userProfile.imageUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                // User Name
                Text(
                    text = userProfile.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                // Email
                Text(
                    text = userProfile.email,
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                // Phone Number (if available)
                if (userProfile.phone.isNotEmpty()) {
                    Text(
                        text = userProfile.phone,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }

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