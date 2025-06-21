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
//import com.google.maps.android.compose.BitmapDescriptorFactory

// Add a data class for shared location info
data class SharedLocationInfo(
    val position: LatLng, 
    val workingStatus: String,
    val receiverName: String,
    val receiverImageUrl: String
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

    // Initialize FusedLocationProviderClient
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Location callback for updating location
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Update location in Firestore for active shared locations
                    val currentUser = FirebaseAuth.getInstance().currentUser?.email
                    if (currentUser != null) {
                        Firebase.firestore.collection("users")
                            .document(currentUser)
                            .collection("shared_locations") 
                            .whereEqualTo("status", "active")
                            .get()
                            .addOnSuccessListener { documents ->
                                for (document in documents) {
                                    // Update the location in the shared_locations document
                                    Firebase.firestore.collection("users")
                                        .document(currentUser)
                                        .collection("shared_locations")
                                        .document(document.id)
                                        .update(
                                            mapOf(
                                                "latitude" to location.latitude,
                                                "longitude" to location.longitude,
                                                "timestamp" to FieldValue.serverTimestamp()
                                            )
                                        )
                                }
                            }
                    }
                }
            }
        }
    }

    // Start location updates when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
            } catch (e: SecurityException) {
                android.util.Log.e("MapScreen", "Error requesting location updates", e)
            }
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
                        if (latitude != null && longitude != null) {
                            sharedLocations = sharedLocations + (document.id to SharedLocationInfo(
                                LatLng(latitude, longitude), 
                                workingStatus, 
                                receiverName, 
                                receiverImageUrl
                            ))
                            Log.d("SharedLocation", "email: ${document.id}, Location: $latitude, $longitude, Status: $workingStatus, Name: $receiverName")
                        }
                    }
                }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            locationListener?.remove()
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // State for selected marker
    var selectedLocation by remember { mutableStateOf<SharedLocationInfo?>(null) }

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
            Marker(
                state = rememberMarkerState(position = info.position),
                title = if (info.receiverName.isNotEmpty()) info.receiverName else email,
                snippet = "Status: ${info.workingStatus}",
                icon = color,
                onClick = {
                    selectedLocation = info
                    false // Let the map handle default behavior (optional)
                }
            )
        }
    }

    // Custom info window dialog
    selectedLocation?.let { info ->
        Dialog(onDismissRequest = { selectedLocation = null }) {
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { selectedLocation = null }) {
                        Text("Close")
                    }
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
