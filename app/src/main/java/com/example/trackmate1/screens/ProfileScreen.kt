package com.example.trackmate1.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.trackmate1.NavigationItems
import com.example.trackmate1.ui.theme.Trackmate1Theme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


@Composable
fun ProfileScreen(navController: NavHostController){
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()){
        Column(modifier = Modifier.fillMaxSize().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
           Button(onClick = {
//               Firebase.auth.signOut()
               signOut(
                   context,
                   webClientId = "9964737077-gp26ao3ddcl27dni79sd6ugkkf1hpn2p.apps.googleusercontent.com"
               ){
                   Toast.makeText(context,"Signed Out",Toast.LENGTH_SHORT).show()

                   navController.navigate(NavigationItems.Login.route){
                       popUpTo(0)
                   }
               }

               navController.navigate(NavigationItems.Login.route){
                   popUpTo(0)
               }
           }){
               Text(text = "Sign Out", color = Color.White, fontSize = 20.sp)
           }

        }
    }
}

fun signOut(context: Context,webClientId:String,onComplete: () -> Unit) {
Firebase.auth.signOut()
    val googeSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context,googeSignInOption)
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