package com.example.trackmate1.screens

import android.annotation.SuppressLint
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
        }


    // Custom info window dialog
    selectedLocation?.let { info ->
        val email = selectedLocationEmail ?: return@let
        Dialog(onDismissRequest = { 
            selectedLocation = null 
            selectedLocationEmail = null 
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (info.receiverImageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(info.receiverImageUrl),
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(text = info.receiverName, modifier = Modifier.padding(bottom = 8.dp))
                    Text(text = "Status: ${info.workingStatus}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (info.onlineStatus) "Online" else "Offline",
                        color = if (info.onlineStatus) Color.Green else Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Call button if phone number is available
                    if (!info.receiverPhoneNum.isNullOrBlank() && info.receiverPhoneNum != "null") {
                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${info.receiverPhoneNum}")
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        selectedLocation = null 
                        selectedLocationEmail = null 
                    }) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AndroidView(
                        factory = { context ->
                            ZegoSendCallInvitationButton(context).apply {
                                setIsVideoCall(true)
                                resourceID = "zego_uikit_call"
                                layoutParams = ViewGroup.LayoutParams(180, 180) // 60dp in px
                            }
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .padding(end = 20.dp),
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
                        modifier = Modifier
                            .size(60.dp)
                            .padding(end = 20.dp),
                        update = { button ->
                            button.setInvitees(
                                listOf(ZegoUIKitUser(email, email))
                            )
                        }
                    )
                }
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