package com.example.trackmate1.screens

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.trackmate1.MainActivity
import com.example.trackmate1.NavigationItems
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.Firebase

import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService

//import kotlin.coroutines.jvm.internal.CompletedContinuation.context

class PreferenceManager(context: Context) {

    private val prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

    fun setLoginStatus(isLoggedIn: Boolean) {
        prefs.edit().putBoolean("login_status", isLoggedIn).apply()
    }

    fun getLoginStatus(): Boolean {
        return prefs.getBoolean("login_status", false)
    }
}



@SuppressLint("SuspiciousIndentation")
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val prefManager = remember { PreferenceManager(context) }

    // ✅ Initialize isLoggedIn with the value from SharedPreferences
    var isLoggedIn by remember { mutableStateOf(prefManager.getLoginStatus()) }

    // ✅ Check for a logged-in user and redirect immediately if needed
    LaunchedEffect(Unit) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (isLoggedIn && firebaseUser != null) {
            navController.navigate(NavigationItems.Search.route) {
                popUpTo("login") { inclusive = true }
            }
        } else {
            // If pref says logged in but Firebase user is null, reset pref
            if (isLoggedIn && firebaseUser == null) {
                prefManager.setLoginStatus(false)
                isLoggedIn = false
            }
        }
    }

    if (isLoggedIn && FirebaseAuth.getInstance().currentUser != null) {
        // No need for a second LaunchedEffect or a return statement here.
        // The first one handles navigation. This just prevents the rest of the code from executing.
        return
    }

    // ... (rest of the code for the login button and UI) ...
    val googleSignInOption = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("9964737077-gp26ao3ddcl27dni79sd6ugkkf1hpn2p.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, googleSignInOption)
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            Firebase.auth.signInWithCredential(credential)
                .addOnCompleteListener { Task ->
                    if (Task.isSuccessful) {
                        Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                        isLoggedIn = true
                        prefManager.setLoginStatus(true)
                        val currentUser = FirebaseAuth.getInstance().currentUser

                        if (currentUser != null) {
                            // ... (rest of your Firebase/Firestore and Zego logic) ...
                            val db = Firebase.firestore
                            val name = currentUser?.displayName.toString();
                            val email = currentUser?.email.toString();
                            val phoneNumber = currentUser?.phoneNumber.toString();
                            val image = currentUser?.photoUrl.toString();
                            val appId: Long = 2057916400
                            val appSignin: String = "3b9c161194ee177099ee4febdb5dc7816f07c3ae278b53af949ae36e458594e9"
                            val userName: String = email
                            val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()

                            (context as? MainActivity)?.saveFcmTokenToFirestore()
                            if (email != null) {
                                db.collection("users")
                                    .document(email)
                                    .get()
                                    .addOnSuccessListener { existingDoc ->
                                        if (existingDoc.exists()) {
                                            val existingPhone = existingDoc.getString("phone")
                                            val existingName = existingDoc.getString("Name")
                                            val existingImageUrl = existingDoc.getString("imageUrl")
                                            val updateData = mutableMapOf<String, Any>()

                                            if (!name.isNullOrBlank() && name != "null" && name != existingName) {
                                                updateData["Name"] = name
                                            }
                                            if (!phoneNumber.isNullOrBlank() && phoneNumber != "null" &&
                                                (existingPhone.isNullOrBlank() || existingPhone == "null")) {
                                                updateData["phone"] = phoneNumber
                                            }
                                            if (!image.isNullOrBlank() && image != "null" && image != existingImageUrl) {
                                                updateData["imageUrl"] = image
                                            }

                                            if (updateData.isNotEmpty()) {
                                                db.collection("users")
                                                    .document(email)
                                                    .update(updateData)
                                                    .addOnSuccessListener {
                                                        Log.d("firestore", "Updated existing user data: $updateData")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.w("firestore", "Error updating existing user data", e)
                                                    }
                                            }

                                            db.collection("users").document("Admin").get()
                                                .addOnSuccessListener { adminDoc ->
                                                    val adminEmail = adminDoc.getString("email")
                                                    val isAdmin = (email == adminEmail)
                                                    val currentIsAdmin = existingDoc.getBoolean("isAdmin") ?: false
                                                    if (isAdmin != currentIsAdmin) {
                                                        db.collection("users").document(email)
                                                            .update("isAdmin", isAdmin)
                                                    }
                                                }
                                        } else {
                                            val user = hashMapOf(
                                                "Name" to name,
                                                "email" to email,
                                                "imageUrl" to image
                                            )
                                            if (!phoneNumber.isNullOrBlank() && phoneNumber != "null") {
                                                user["phone"] = phoneNumber
                                            }
                                            db.collection("users")
                                                .document(email)
                                                .set(user, com.google.firebase.firestore.SetOptions.merge())
                                                .addOnSuccessListener {
                                                    Log.d("firestore", "Created new user document")
                                                    db.collection("users").document("Admin").get()
                                                        .addOnSuccessListener { adminDoc ->
                                                            val adminEmail = adminDoc.getString("email")
                                                            val isAdmin = (email == adminEmail)
                                                            db.collection("users").document(email)
                                                                .update("isAdmin", isAdmin)
                                                        }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w("firestore", "Error creating new user document", e)
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("firestore", "Error checking existing user document", e)
                                    }
                            } else {
                                Log.e("firestore", "User email is null, skipping Firestore write")
                            }

                            navController.navigate(NavigationItems.Search.route) {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            Log.w("SignIn", "signInWithCredential:failure", Task.exception)
                            Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = if (isLoggedIn) "Welcome Back!" else "Please Log In")
        AndroidView(modifier = Modifier.fillMaxWidth().height(48.dp),
            factory = { context ->
                SignInButton(context).apply {
                    setSize(SignInButton.SIZE_WIDE)
                    setOnClickListener {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    }
                }
            })
    }
}