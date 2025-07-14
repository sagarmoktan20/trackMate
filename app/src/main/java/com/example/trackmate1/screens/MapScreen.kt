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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
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
import androidx.compose.ui.viewinterop.AndroidView
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

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
    var clientLat by remember { mutableStateOf("") }
    var clientLng by remember { mutableStateOf("") }
    
    // State for accepted task (client marker)
    var acceptedTaskClientName by remember { mutableStateOf<String?>(null) }
    var acceptedTaskClientPhone by remember { mutableStateOf<String?>(null) }
    var acceptedTaskClientLat by remember { mutableStateOf<Double?>(null) }
    var acceptedTaskClientLng by remember { mutableStateOf<Double?>(null) }
    var hasAcceptedTask by remember { mutableStateOf(false) }

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
            hasAcceptedTask = true
            
            // Clear the shared preferences after reading
            prefs.edit().clear().apply()
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
        }


    // Custom info window dialog
    selectedLocation?.let { info ->
        val email = selectedLocationEmail ?: return@let
        Dialog(onDismissRequest = { 
            selectedLocation = null 
            selectedLocationEmail = null 
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
                    
                    OutlinedTextField(
                        value = clientLat,
                        onValueChange = { clientLat = it },
                        label = { Text("Client Latitude") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = clientLng,
                        onValueChange = { clientLng = it },
                        label = { Text("Client Longitude") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Validate inputs
                        if (clientName.isBlank() || clientPhone.isBlank() || clientLat.isBlank() || clientLng.isBlank()) {
                            android.widget.Toast.makeText(context, "Please fill all fields", android.widget.Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        
                        // Validate coordinates
                        val lat = clientLat.toDoubleOrNull()
                        val lng = clientLng.toDoubleOrNull()
                        if (lat == null || lng == null) {
                            android.widget.Toast.makeText(context, "Please enter valid coordinates", android.widget.Toast.LENGTH_SHORT).show()
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
                            "clientLat" to lat,
                            "clientLng" to lng,
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
                                clientLat = ""
                                clientLng = ""
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
                        clientLat = ""
                        clientLng = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview
@Composable
fun MapScreenPreview() {
    Trackmate1Theme {
        MapScreen()
    }
}