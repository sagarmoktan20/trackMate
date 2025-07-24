package com.example.trackmate1.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.media3.common.util.Log
import com.example.trackmate1.ui.theme.Trackmate1Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
//import com.google.android.gms.location.LocationResult
//import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FieldValue
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.Image
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.ui.viewinterop.AndroidView
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.withContext
import com.google.maps.android.compose.Polyline
import com.google.maps.android.PolyUtil
import android.location.Location

//import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoIncomingCallInvitationButton

//import com.google.maps.android.compose.BitmapDescriptorFactory

// Add a data class for shared location info
data class SharedLocationInfo(
    val position: LatLng,
    val workingStatus: String,
    val receiverName: String,
    val receiverImageUrl: String,
    val onlineStatus: Boolean,
    val receiverPhoneNum: String? = null
)

@OptIn(UnstableApi::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(27.7172, 85.3240), 10f)
    }

    val mapProperties = MapProperties(
        isMyLocationEnabled = hasPermission
    )

    // State for shared locations (now includes working status)
    var sharedLocations by remember { mutableStateOf(mapOf<String, SharedLocationInfo>()) }
    var locationListener: ListenerRegistration? by remember { mutableStateOf(null) }
    
    // Admin detection state
    var isAdmin by remember { mutableStateOf(false) }
    
    // Task assignment dialog state
    var showTaskAssignmentDialog by remember { mutableStateOf(false) }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    // Remove clientLat and clientLng text state
    // Add state for location picker dialog and selected coordinates
    var showLocationPickerDialog by remember { mutableStateOf(false) }
    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLng by remember { mutableStateOf<Double?>(null) }
    
    // State for accepted task (client marker)
    var acceptedTaskClientName by remember { mutableStateOf<String?>(null) }
    var acceptedTaskClientPhone by remember { mutableStateOf<String?>(null) }
    var acceptedTaskClientLat by remember { mutableStateOf<Double?>(null) }
    var acceptedTaskClientLng by remember { mutableStateOf<Double?>(null) }
    var hasAcceptedTask by remember { mutableStateOf(false) }
    var acceptedTaskId by remember { mutableStateOf<String?>(null) }

    // State for storing our current location for routing
    var myCurrentLat by remember { mutableStateOf<Double?>(null) }
    var myCurrentLng by remember { mutableStateOf<Double?>(null) }

    // State for last API call location
    var lastApiLat by remember { mutableStateOf<Double?>(null) }
    var lastApiLng by remember { mutableStateOf<Double?>(null) }

    // State for decoded route points
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Polyline decoder function (replace with PolyUtil.decode)
    // fun decodePolyline(encoded: String): List<LatLng> { ... }

    // LocationCallback reference for cleanup
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    myCurrentLat = location.latitude
                    myCurrentLng = location.longitude
                    Log.d("ROUTE_DEBUG", "Current location: ${location.latitude}, ${location.longitude}")
                }
            }
        }
    }

    // Start/stop continuous location updates based on hasAcceptedTask
    DisposableEffect(hasAcceptedTask) {
        if (hasAcceptedTask && hasPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000L)
                .setMinUpdateIntervalMillis(15000L)
                .build()
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                context.mainLooper
            )
        }
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Check if current user is admin
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser?.email
        if (currentUser != null) {
            Firebase.firestore.collection("users")
                .document(currentUser)
                .get()
                .addOnSuccessListener { document ->
                    isAdmin = document.getBoolean("isAdmin") ?: false
                }
        }
    }
    
    // Check for accepted task when MapScreen loads
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("task_prefs", Context.MODE_PRIVATE)
        val hasTask = prefs.getBoolean("has_accepted_task", false)//if empty defaults to false
        if (hasTask) {
            acceptedTaskClientName = prefs.getString("accepted_client_name", null)
            acceptedTaskClientPhone = prefs.getString("accepted_client_phone", null)
            acceptedTaskClientLat = prefs.getFloat("accepted_client_lat", 0f).toDouble()
            acceptedTaskClientLng = prefs.getFloat("accepted_client_lng", 0f).toDouble()
            acceptedTaskId = prefs.getString("accepted_task_id", null)
            hasAcceptedTask = true
        }
    }

    // Listen for shared locations
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser?.email
        if (currentUser != null) {
            locationListener = Firebase.firestore.collection("users")
                .document(currentUser)
                .collection("shared_locations")
                .whereEqualTo("status", "active")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        android.util.Log.e("MapScreen", "Error listening for location updates", e)
                        return@addSnapshotListener
                    }

                    snapshot?.documents?.forEach { document ->
                        val latitude = document.getDouble("latitude")
                        val longitude = document.getDouble("longitude")
                        val workingStatus = document.getString("working_status") ?: "Free"
                        val receiverName = document.getString("receiver_name") ?: ""
                        val receiverImageUrl = document.getString("receiver_imageUrl") ?: ""
                        val onlineStatus = document.getBoolean("online_status") ?: false
                        val receiverPhoneNum = document.getString("receiver_phone")
                        if (latitude != null && longitude != null) {
                            sharedLocations = sharedLocations + (document.id to SharedLocationInfo(
                                LatLng(latitude, longitude),
                                workingStatus,
                                receiverName,
                                receiverImageUrl,
                                onlineStatus,
                                receiverPhoneNum
                            ))
                            Log.d("SharedLocation", "email: ${document.id}, Location: $latitude, $longitude, Status: $workingStatus, Name: $receiverName, Phone: $receiverPhoneNum, Online: $onlineStatus")
                        }
                    }
                }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            locationListener?.remove()
        }
    }

    // State for selected marker and email
    var selectedLocation by remember { mutableStateOf<SharedLocationInfo?>(null) }
    var selectedLocationEmail by remember { mutableStateOf<String?>(null) }
    // Add state for latest phone number and loading
    var latestPhoneNum by remember { mutableStateOf<String?>(null) }
    var isPhoneLoading by remember { mutableStateOf(false) }

//    Box(modifier = Modifier.fillMaxSize()) {
//        // 📞 Incoming Call UI (hidden by default, shows when receiving calls)
//        AndroidView(
//            factory = { context ->
//                ZegoIncomingCallInvitationButton(context).apply {
//                    resourceID = "zego_uikit_incoming_call"
//                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
//                }
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(200.dp)
//                .padding(vertical = 8.dp)
//        )

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties
        ) {
            // Add markers for shared locations
            sharedLocations.forEach { (email, info) ->
                val color = when (info.workingStatus) {
                    "Busy" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    "Free" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    "En-route" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                    else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                }
                val onlineStatusEmoji = if (info.onlineStatus) "🟢" else "🔴"
                val onlineStatusText = if (info.onlineStatus) "Online" else "Offline"
                Marker(
                    state = rememberMarkerState(position = info.position),
                    title = if (info.receiverName.isNotEmpty()) "${info.receiverName} ($onlineStatusEmoji $onlineStatusText)" else "$email ($onlineStatusEmoji $onlineStatusText)",
                    snippet = "Status: ${info.workingStatus}\nOnline: $onlineStatusText",
                    icon = color,
                    onClick = {
                        selectedLocation = info
                        selectedLocationEmail = email
                        // Fetch latest phone number from Firestore
                        isPhoneLoading = true
                        latestPhoneNum = null
                        Firebase.firestore.collection("users")
                            .document(email)
                            .get()
                            .addOnSuccessListener { document ->
                                latestPhoneNum = document.getString("phone")
                                isPhoneLoading = false
                            }
                            .addOnFailureListener {
                                latestPhoneNum = null
                                isPhoneLoading = false
                            }
                        false // Let the map handle default behavior (optional)
                    }
                )
            }
            
            // Add client marker if there's an accepted task
            if (hasAcceptedTask && acceptedTaskClientLat != null && acceptedTaskClientLng != null) {
                Marker(
                    state = rememberMarkerState(position = LatLng(acceptedTaskClientLat!!, acceptedTaskClientLng!!)),
                    title = "Client: ${acceptedTaskClientName ?: "Unknown"}",
                    snippet = "Phone: ${acceptedTaskClientPhone ?: "N/A"}\nStatus: Accepted Task",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                    onClick = {
                        // Show client info dialog
                        selectedLocation = SharedLocationInfo(
                            position = LatLng(acceptedTaskClientLat!!, acceptedTaskClientLng!!),
                            workingStatus = "Client Location",
                            receiverName = acceptedTaskClientName ?: "Unknown Client",
                            receiverImageUrl = "",
                            onlineStatus = true,
                            receiverPhoneNum = acceptedTaskClientPhone
                        )
                        selectedLocationEmail = "CLIENT_TASK"
                        false
                    }
                )
            }

            // Draw the polyline if routePoints is not empty
            if (routePoints.isNotEmpty()) {
                // Draw a dark gray border (halo) first
                Polyline(
                    points = routePoints,
                    color = Color(0xFF222222), // Dark gray
                    width = 24f
                )
                // Draw the main blue route on top
                Polyline(
                    points = routePoints,
                    color = Color(0xFF4285F4), // Google Blue
                    width = 16f
                )
            }
        }


    // Custom info window dialog
    selectedLocation?.let { info ->
        val email = selectedLocationEmail ?: return@let
        Dialog(onDismissRequest = { 
            selectedLocation = null 
            selectedLocationEmail = null 
            latestPhoneNum = null
            isPhoneLoading = false
        }) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                color = androidx.compose.ui.graphics.Color.White,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Check if this is a client task marker
                    if (email == "CLIENT_TASK") {
                        // Client Task Dialog - Simplified
                        Text(
                            text = info.receiverName, 
                            modifier = Modifier.padding(bottom = 16.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            color = androidx.compose.ui.graphics.Color.Black
                        )
                        
                        // Show assigned by info
                        Text(
                            text = "Assigned by: ${acceptedTaskClientName?.let { "Admin" } ?: "Unknown"}",
                            modifier = Modifier.padding(bottom = 16.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                        
                        // Phone call button
                        if (!info.receiverPhoneNum.isNullOrBlank() && info.receiverPhoneNum != "null") {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${info.receiverPhoneNum}")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call ${info.receiverName}")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        // Completed button
                        Button(
                            onClick = {
                                // Delete the accepted task from Firestore
                                val currentUser = FirebaseAuth.getInstance().currentUser?.email
                                android.util.Log.d("CompletedButton", "currentUser: $currentUser")
                                val prefs = context.getSharedPreferences("task_prefs", Context.MODE_PRIVATE)
                                val acceptedTaskId = prefs.getString("accepted_task_id", null)
                                android.util.Log.d("CompletedButton", "acceptedTaskId: $acceptedTaskId")
                                if (currentUser != null && acceptedTaskId != null) {
                                    android.util.Log.d("CompletedButton", "Entered IF: Deleting task from Firestore")
                                    com.google.firebase.Firebase.firestore.collection("users")
                                        .document(currentUser)
                                        .collection("assigned_tasks")
                                        .document(acceptedTaskId)
                                        .delete()
                                        .addOnSuccessListener {
                                            android.util.Log.d("CompletedButton", "Task deleted successfully from Firestore")
                                            android.widget.Toast.makeText(context, "Task completed and removed!", android.widget.Toast.LENGTH_SHORT).show()
                                            // Clear SharedPreferences
                                            prefs.edit().clear().apply()
                                            // Reset local state
                                            acceptedTaskClientName = null
                                            acceptedTaskClientPhone = null
                                            acceptedTaskClientLat = null
                                            acceptedTaskClientLng = null
                                            hasAcceptedTask = false
                                            // Clear the route as well
                                            routePoints = emptyList()
                                            // Close dialog
                                            selectedLocation = null
                                            selectedLocationEmail = null
                                        }
                                        .addOnFailureListener { e ->
                                            android.util.Log.e("CompletedButton", "Failed to delete task: ${e.message}")
                                            android.widget.Toast.makeText(context, "Failed to remove task: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    android.util.Log.d("CompletedButton", "Entered ELSE: Either currentUser or acceptedTaskId is null")
                                    // Fallback: just clear state if no taskId
                                    prefs.edit().clear().apply()
                                    acceptedTaskClientName = null
                                    acceptedTaskClientPhone = null
                                    acceptedTaskClientLat = null
                                    acceptedTaskClientLng = null
                                    hasAcceptedTask = false
                                    routePoints = emptyList()
                                    selectedLocation = null
                                    selectedLocationEmail = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF388E3C) // Green
                            )
                        ) {
                            Text("Completed", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Close button
                        Button(
                            onClick = { 
                                selectedLocation = null 
                                selectedLocationEmail = null 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    } else {
                        // Regular Employee Dialog - Full features
                        // 1. Profile Image at top
                        if (info.receiverImageUrl.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(info.receiverImageUrl),
                                contentDescription = "Profile Image",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                        
                        // 2. Name below image
                        Text(
                            text = info.receiverName, 
                            modifier = Modifier.padding(bottom = 16.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            color = androidx.compose.ui.graphics.Color.Black
                        )
                        
                        // 3. Status and online status below name
                        Text(
                            text = "Status: ${info.workingStatus}",
                            modifier = Modifier.padding(bottom = 8.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            color = androidx.compose.ui.graphics.Color.Black
                        )
                        Text(
                            text = if (info.onlineStatus) "Online" else "Offline",
                            color = if (info.onlineStatus) Color.Green else Color.Red,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                        
                        // 4. ZegoCloud video and audio call buttons side by side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
                        ) {
                            // Video Call Button
                            AndroidView(
                                factory = { context ->
                                    ZegoSendCallInvitationButton(context).apply {
                                        setIsVideoCall(true)
                                        resourceID = "zego_uikit_call"
                                        layoutParams = ViewGroup.LayoutParams(180, 180)
                                    }
                                },
                                modifier = Modifier.size(60.dp),
                                update = { button ->
                                    button.setInvitees(
                                        listOf(ZegoUIKitUser(email, email))
                                    )
                                }
                            )
                            
                            // Voice Call Button
                            AndroidView(
                                factory = { context ->
                                    ZegoSendCallInvitationButton(context).apply {
                                        setIsVideoCall(false)
                                        resourceID = "zego_uikit_call"
                                        layoutParams = ViewGroup.LayoutParams(180, 180)
                                    }
                                },
                                modifier = Modifier.size(60.dp),
                                update = { button ->
                                    button.setInvitees(
                                        listOf(ZegoUIKitUser(email, email))
                                    )
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 5. Offline SIM calling button below ZegoCloud buttons
                        if (isPhoneLoading) {
                            androidx.compose.material3.CircularProgressIndicator()
                        } else if (!latestPhoneNum.isNullOrBlank() && latestPhoneNum != "null") {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${latestPhoneNum}")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call ${info.receiverName}")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // 6. Assign Task button (only for admins)
                        if (isAdmin) {
                            Button(
                                onClick = {
                                    showTaskAssignmentDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.Blue
                                )
                            ) {
                                Text("Assign Task")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // 7. Close button at the bottom
                        Button(
                            onClick = { 
                                selectedLocation = null 
                                selectedLocationEmail = null 
                                latestPhoneNum = null
                                isPhoneLoading = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
    
    // Task Assignment Dialog
    if (showTaskAssignmentDialog) {
        AlertDialog(
            onDismissRequest = { showTaskAssignmentDialog = false },
            title = { Text("Assign Task") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Client Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Button(
                        onClick = {
                            showLocationPickerDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Assign Location")
                    }
                    // Show picked coordinates if available
                    if (selectedLat != null && selectedLng != null) {
                        Text("Selected: ${String.format("%.5f", selectedLat)}, ${String.format("%.5f", selectedLng)}")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Validate inputs
                        if (clientName.isBlank() || clientPhone.isBlank() || selectedLat == null || selectedLng == null) {
                            android.widget.Toast.makeText(context, "Please fill all fields and pick a location", android.widget.Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        // Get employee email from selected location
                        val employeeEmail = selectedLocationEmail
                        if (employeeEmail == null) {
                            android.widget.Toast.makeText(context, "Error: Employee not found", android.widget.Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        // Get current admin user
                        val currentUser = FirebaseAuth.getInstance().currentUser?.email
                        if (currentUser == null) {
                            android.widget.Toast.makeText(context, "Error: Admin not authenticated", android.widget.Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        // Create task data
                        val taskData = hashMapOf(
                            "clientName" to clientName,
                            "clientPhone" to clientPhone,
                            "clientLat" to selectedLat,
                            "clientLng" to selectedLng,
                            "assignedBy" to currentUser,
                            "status" to "pending",
                            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        // Save to Firestore
                        Firebase.firestore.collection("users")
                            .document(employeeEmail)
                            .collection("assigned_tasks")
                            .add(taskData)
                            .addOnSuccessListener { documentReference ->
                                android.widget.Toast.makeText(context, "Task assigned successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                showTaskAssignmentDialog = false
                                // Clear the form
                                clientName = ""
                                clientPhone = ""
                                selectedLat = null
                                selectedLng = null
                            }
                            .addOnFailureListener { e ->
                                android.widget.Toast.makeText(context, "Failed to assign task: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    }
                ) {
                    Text("Assign")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTaskAssignmentDialog = false
                        // Clear the form
                        clientName = ""
                        clientPhone = ""
                        selectedLat = null
                        selectedLng = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Location Picker Dialog
    if (showLocationPickerDialog) {
        Dialog(onDismissRequest = { showLocationPickerDialog = false }) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth(0.98f)
                    .heightIn(min = 500.dp, max = 700.dp)
                    .padding(8.dp),
                color = androidx.compose.ui.graphics.Color.White,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                var pickerLatLng by remember { mutableStateOf<LatLng?>(null) }
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(LatLng(27.7172, 85.3240), 10f)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = 500.dp, max = 700.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Pick a location", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier
                        .height(400.dp)
                        .fillMaxWidth()) {
                        GoogleMap(
                            modifier = Modifier.matchParentSize(),
                            cameraPositionState = cameraPositionState,
                            onMapClick = { latLng ->
                                pickerLatLng = latLng
                            }
                        ) {
                            pickerLatLng?.let { latLng ->
                                Marker(
                                    state = rememberMarkerState(position = latLng),
                                    title = "Selected Location"
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            showLocationPickerDialog = false
                        }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (pickerLatLng != null) {
                                    selectedLat = pickerLatLng!!.latitude
                                    selectedLng = pickerLatLng!!.longitude
                                    showLocationPickerDialog = false
                                } else {
                                    android.widget.Toast.makeText(context, "Please pick a location", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = pickerLatLng != null
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }

    // Build the Directions API URL only if user has moved more than 5 meters
    val apiKey = "AIzaSyAnCeP9vtWPicCvHYg4jwzawvDyJ8WxCfw"
    var directionsUrl by remember { mutableStateOf<String?>(null) }

    // Function to calculate distance between two lat/lng points in meters
    fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }

    // Watch for location changes and trigger API call if moved > 20 meters
    LaunchedEffect(myCurrentLat, myCurrentLng, acceptedTaskClientLat, acceptedTaskClientLng) {
        if (
            myCurrentLat != null && myCurrentLng != null &&
            acceptedTaskClientLat != null && acceptedTaskClientLng != null
        ) {
            if (lastApiLat != null && lastApiLng != null) {
                val distance = distanceBetween(myCurrentLat!!, myCurrentLng!!, lastApiLat!!, lastApiLng!!)
                Log.d("ROUTE_DEBUG", "Distance from last API call: $distance meters")
                if (distance > 20f) {
                    val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${myCurrentLat},${myCurrentLng}&destination=${acceptedTaskClientLat},${acceptedTaskClientLng}&key=$apiKey"
                    Log.d("ROUTE_DEBUG", "Directions API URL: $url")
                    directionsUrl = url
                    lastApiLat = myCurrentLat
                    lastApiLng = myCurrentLng
                }
            } else {
                // First time, always make the call
                val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${myCurrentLat},${myCurrentLng}&destination=${acceptedTaskClientLat},${acceptedTaskClientLng}&key=$apiKey"
                Log.d("ROUTE_DEBUG", "Directions API URL: $url (first call)")
                directionsUrl = url
                lastApiLat = myCurrentLat
                lastApiLng = myCurrentLng
            }
        }
    }

    // Make the HTTP request to the Directions API when the URL is available
    LaunchedEffect(directionsUrl) {
        if (directionsUrl != null) {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(directionsUrl!!).build()
            try {
                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                val json = response.body?.string()
                Log.d("ROUTE_DEBUG", "Directions API response: $json")

                // Parse the JSON and extract the polyline points string
                if (json != null) {
                    try {
                        val jsonObject = org.json.JSONObject(json)
                        val routesArray = jsonObject.getJSONArray("routes")
                        if (routesArray.length() > 0) {
                            val route = routesArray.getJSONObject(0)
                            val overviewPolyline = route.getJSONObject("overview_polyline")
                            val polylinePoints = overviewPolyline.getString("points")
                            Log.d("ROUTE_DEBUG", "Extracted polyline points: $polylinePoints")
                            // Decode polyline and update state
                            routePoints = PolyUtil.decode(polylinePoints)
                        } else {
                            Log.e("ROUTE_DEBUG", "No routes found in Directions API response")
                        }
                    } catch (e: Exception) {
                        Log.e("ROUTE_DEBUG", "Failed to parse polyline from Directions API response: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ROUTE_DEBUG", "Directions API request failed: ${e.message}")
            }
        }
    }
}

@Preview
@Composable
fun MapScreenPreview() {
    Trackmate1Theme {
        MapScreen()
    }
}