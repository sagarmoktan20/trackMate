package com.example.trackmate1.screens

import android.annotation.SuppressLint
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
import com.example.trackmate1.NavigationItems
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.Firebase

import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

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
fun LoginScreen(navController:NavHostController){
    val context = LocalContext.current;
    val prefManager = remember { PreferenceManager(context) }
//    var isLoggedIn by remember { mutableStateOf(prefManager.getLoginStatus()) }

    var isLoggedIn by remember { mutableStateOf(false) }

    // ✅ Always check latest login status
    LaunchedEffect(Unit) {
        isLoggedIn = prefManager.getLoginStatus()
    }

    if (isLoggedIn) {
        LaunchedEffect(Unit) {
            navController.navigate(NavigationItems.Search.route) {
                popUpTo("login") { inclusive = true }
            }
        }
        return
    }

    val googleSignInOption = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("9964737077-gp26ao3ddcl27dni79sd6ugkkf1hpn2p.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context,googleSignInOption)
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {result->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken,null)
            Firebase.auth.signInWithCredential(credential)
                .addOnCompleteListener {Task->
                    if (Task.isSuccessful){
                        Toast.makeText(context,"Login Successful",Toast.LENGTH_SHORT).show()
                        isLoggedIn = true
                        prefManager.setLoginStatus(true);
                        val currentUser = FirebaseAuth.getInstance().currentUser;

                        if(currentUser!= null){
                            val db = Firebase.firestore
                            val name = currentUser?.displayName.toString();
                            val email = currentUser?.email.toString();
                            val phoneNumber = currentUser?.phoneNumber.toString();
                            val image = currentUser?.photoUrl.toString();


                            if (email != null) {
                                // Create a new user with a first and last name
                                val user = hashMapOf(
                                    "Name" to name,
                                    "email" to email,
                                    "phone" to phoneNumber,
                                    "imageUrl" to image
                                )

// Add a new document with a generated ID
                                db.collection("users")
                                    .document(email)
                                    .set(user)
                                    .addOnSuccessListener { documentReference ->
                                        // Fetch admin email and set isAdmin field
                                        db.collection("users").document("Admin").get()
                                            .addOnSuccessListener { adminDoc ->
                                                val adminEmail = adminDoc.getString("email")
                                                val isAdmin = (email == adminEmail)
                                                db.collection("users").document(email)
                                                    .update("isAdmin", isAdmin)
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("fire1", "Error adding document", e)
                                    }}else {
                                Log.e("firestore", "User email is null, skipping Firestore write")
                            }

                            navController.navigate(NavigationItems.Search.route){
                                popUpTo("login") {inclusive = true}
                            }

                        }else{
                            Log.w("SignIn", "signInWithCredential:failure", task.exception)
                            Toast.makeText(context,"Login Failed",Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(context,"Login Failed",Toast.LENGTH_SHORT).show()
                    }
                }
        }catch (e:Exception){
            Toast.makeText(context,"Login Failed",Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = if (isLoggedIn) "Welcome Back!" else "Please Log In")

        AndroidView(modifier = Modifier.fillMaxWidth().height(48.dp),
            factory = {context->
                SignInButton(context).apply {
                    setSize(SignInButton.SIZE_WIDE)
                    setOnClickListener {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)

                    }
                }
            })}
}