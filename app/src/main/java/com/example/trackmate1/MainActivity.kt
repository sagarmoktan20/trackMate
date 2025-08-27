package com.example.trackmate1

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.trackmate1.screens.LoginScreen
import com.example.trackmate1.screens.MapScreen
import com.example.trackmate1.screens.ProfileScreen
import com.example.trackmate1.screens.SearchScreen
import com.example.trackmate1.ui.theme.Trackmate1Theme
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService




//    private val locationPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
//        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
//
//        if (fineLocationGranted || coarseLocationGranted)
//        {
//              if(isLocationEnabled(this))
//              {
//                          setUpLocationListener()
//                }else
//                {
//                     showGPSNotEnabledDialog(this)
//                }
//         } else {
//        askForPermission()
//        }
//    }
class MainActivity : ComponentActivity() {


    private val permissionsToRequest = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Add function to save initial FCM token
    fun saveFcmTokenToFirestore() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM_TOKEN", "Initial FCM Token: $token")
                    Firebase.firestore.collection("users")
                        .document(user.email ?: return@addOnCompleteListener)
                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("FCM_TOKEN", "Initial FCM token saved to Firestore successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FCM_TOKEN", "Failed to save initial FCM token: ${e.message}")
                        }
                } else {
                    Log.e("FCM_TOKEN", "Failed to get initial FCM token: ${task.exception}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun setUpLocationListener() {
        Log.d("Location101", "setUpLocationListener called")

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest().setInterval(2000).setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    for (location in locationResult.locations) {

                        Log.d("Location101", "${location.latitude}, ${location.longitude}")

                        val currentUser = FirebaseAuth.getInstance().currentUser;
                        val firestoreDb = Firebase.firestore
                        val Myemail = currentUser?.email.toString();
                        val locationData = mutableMapOf<String, Any>(
                            "lat" to location.latitude.toString(),
                            "long" to location.longitude.toString()
                        )
                        firestoreDb.collection("users")
                            .document(Myemail).update(locationData)

                        // 1. Update sender's shared_locations with my (receiver's) coordinates for each accepted invite
                        firestoreDb.collection("users")
                            .document(Myemail)
                            .collection("invites")
                            .whereEqualTo("invite_status", 1)
                            .get()
                            .addOnSuccessListener { invitesSnapshot ->
                                for (inviteDoc in invitesSnapshot) {
                                    val senderEmail = inviteDoc.id
                                    val sharedLocationData = mapOf(
                                        "latitude" to location.latitude,
                                        "longitude" to location.longitude,
                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                        "online_status" to true
                                    )
                                    firestoreDb.collection("users")
                                        .document(senderEmail)
                                        .collection("shared_locations")
                                        .document(Myemail)
                                        .update(sharedLocationData)
                                }
                            }

                        // 2. Update my shared_locations in the view of users I sent an invite to and they accepted (status == 'active')
                        firestoreDb.collection("users")
                            .document(Myemail)
                            .collection("shared_locations")
                            .whereEqualTo("status", "active")
                            .get()
                            .addOnSuccessListener { sharedLocSnapshot ->
                                for (sharedLocDoc in sharedLocSnapshot) {
                                    val receiverEmail = sharedLocDoc.id
                                    val sharedLocationData = mapOf(
                                        "latitude" to location.latitude,
                                        "longitude" to location.longitude,
                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                        "online_status" to true
                                    )
                                    firestoreDb.collection("users")
                                        .document(receiverEmail)
                                        .collection("shared_locations")
                                        .document(Myemail)
                                        .update(sharedLocationData)
                                }
                            }


//                        latTextView.text = location.latitude.toString()
//                        lngTextView.text = location.longitude.toString()
                    }
                    // Few more things we can do here:
                    // For example: Update the location of user on server
                }
            },
            Looper.myLooper()
        )
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Function to show the "enable GPS" Dialog box
     */
    fun showGPSNotEnabledDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(("Enable GPS"))
            .setMessage("required_for_this_app")
            .setCancelable(false)
            .setPositiveButton("enable_now") { _, _ ->
                context.startActivity(Intent(ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .show()
    }


    //    private val requestCode = 101
//    val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the title bar completely
//        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
//        actionBar?.hide()
//
        enableEdgeToEdge()
//    askForPermission()
        installSplashScreen()
        setContent {
            Trackmate1Theme {
                val viewModel = viewModel<MainViewModel>()
                val dialogQueue = viewModel.visiblePermissionDialogQueue

                val multiplePermissionResultLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { perms ->
                        permissionsToRequest.forEach { permission ->
                            viewModel.onPermissionResult(
                                permission = permission,
                                isGranted = perms[permission] == true
                            )
                        }
                        // After permissions are handled, check for location permission and GPS
                        val locationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        if (locationGranted) {
                            if (!isLocationEnabled(this@MainActivity)) {
                                showGPSNotEnabledDialog(this@MainActivity)
                            }
                        }
                    }
                )

                // Automatically request all permissions on app load
                LaunchedEffect(Unit) {
                    multiplePermissionResultLauncher.launch(permissionsToRequest)
                }


//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }

                // In MainActivity.kt, inside setContent { ... }
// ...
                // val dialogQueue = viewModel.visiblePermissionDialogQueue

// Only show a dialog if the queue is not empty
                if (dialogQueue.isNotEmpty()) {
                    Log.d("DIALOG_DEBUG", "Dialog queue is NOT empty: $dialogQueue")
                    val permissionToShow = dialogQueue.reversed().first()
                    val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        permissionToShow
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    val isPermanentlyDeclined = !isGranted && !shouldShowRequestPermissionRationale(permissionToShow)
                    Log.d("should", "Dialog queue: $dialogQueue, permissionToShow: $permissionToShow, isGranted: $isGranted, isPermanentlyDeclined: $isPermanentlyDeclined")

                    PermissionDialog(
                        permissionTextProvider = when (permissionToShow) {
                            Manifest.permission.CAMERA -> CameraPermissionTextProvider()
                            Manifest.permission.RECORD_AUDIO -> RecordAudioPermissionTextProvider()
                            Manifest.permission.ACCESS_FINE_LOCATION -> FineLocationPermissionTextProvider()
                            Manifest.permission.ACCESS_COARSE_LOCATION -> CoarseLocationPermissionTextProvider()
                            else -> return@Trackmate1Theme
                        },
                        isPermanentlyDeclined = isPermanentlyDeclined,
                        onDismiss = viewModel::dismissDialog,
                        onOkClick = {
                            viewModel.dismissDialog()
                            multiplePermissionResultLauncher.launch(
                                arrayOf(permissionToShow)
                            )
                        },
                        onGoToAppSettingsClick = ::openAppSettings
                    )
                } else {
                    Log.d("DIALOG_DEBUG", "Dialog queue is empty, no dialog shown")
                }
                MainScreen()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        ZegoUIKitPrebuiltCallInvitationService.unInit()
    }


//    private fun askForPermission() {
//        locationPermissionLauncher.launch(
//            arrayOf(
//                android.Manifest.permission.ACCESS_FINE_LOCATION,
//                android.Manifest.permission.ACCESS_COARSE_LOCATION
//            )
//        )
//    }

}

fun Activity.openAppSettings() {
    android.util.Log.d("SETTINGS_DEBUG", "openAppSettings() called - this should only happen when user clicks 'Go to Settings' in permission dialog")
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}


@Composable
fun MainScreen(){

    val context = LocalContext.current
    val activity = context as MainActivity
    // Removed redundant permissionGranted state and permissionLauncher logic
    // All permission handling is now done in MainActivity's unified flow

    // 🔁 Observe lifecycle (when app returns from settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (activity.isLocationEnabled(context)) {
                    activity.setUpLocationListener()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    val navController = rememberNavController();
    val selected = remember {
        mutableStateOf(Icons.Default.Search)
    }
    val currentRoute = navController
        .currentBackStackEntryAsState().value?.destination?.route

    val bottomBarRoutes = listOf(

        NavigationItems.Search.route,
        NavigationItems.Map.route,
        NavigationItems.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                BottomAppBar(containerColor = Color.Gray, contentColor = Color.White) {
                    // Search
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .clickable {
                                selected.value = Icons.Default.Search
                                navController.navigate(NavigationItems.Search.route) {
                                    popUpTo(0)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(26.dp),
                            tint = if (selected.value == Icons.Default.Search) Color.White else Color.Black
                        )
                        Text(
                            text = "Search",
                            fontSize = 12.sp,
                            color = if (selected.value == Icons.Default.Search) Color.White else Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Map
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .clickable {
                                selected.value = Icons.Default.Menu
                                navController.navigate(NavigationItems.Map.route) {
                                    popUpTo(0)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Map",
                            modifier = Modifier.size(26.dp),
                            tint = if (selected.value == Icons.Default.Menu) Color.White else Color.Black
                        )
                        Text(
                            text = "Map",
                            fontSize = 12.sp,
                            color = if (selected.value == Icons.Default.Menu) Color.White else Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Profile
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .clickable {
                                selected.value = Icons.Default.Person
                                navController.navigate(NavigationItems.Profile.route) {
                                    popUpTo(0)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(26.dp),
                            tint = if (selected.value == Icons.Default.Person) Color.White else Color.Black
                        )
                        Text(
                            text = "Profile",
                            fontSize = 12.sp,
                            color = if (selected.value == Icons.Default.Person) Color.White else Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }

            }
        }
    ) {paddingValues ->
        NavHost(navController = navController,
            startDestination = NavigationItems.Login.route,modifier = Modifier.padding(paddingValues)){
            composable(NavigationItems.Search.route){ SearchScreen() }
            composable(NavigationItems.Map.route){ MapScreen() }
            composable(NavigationItems.Profile.route){ ProfileScreen(navController) }
            composable(NavigationItems.Login.route){ LoginScreen(navController) }
        }
    }

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Trackmate1Theme  {
        MainScreen()
    }
}