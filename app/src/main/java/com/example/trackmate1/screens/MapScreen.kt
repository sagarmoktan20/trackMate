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

    // State for shared locations
    var sharedLocations by remember { mutableStateOf(mapOf<String, LatLng>()) }
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
                        if (latitude != null && longitude != null) {
                            sharedLocations = sharedLocations + (document.id to LatLng(latitude, longitude))
                            Log.d("SharedLocation", "email: ${document.id}, Location: $latitude, $longitude")

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

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties
    ) {
        // Add markers for shared locations
        sharedLocations.forEach { (email, location) ->
            //android.util.Log.d("SharedLocation", "Email: $email, Location: ${location.latitude}, ${location.longitude}")
            Marker(

                state = rememberMarkerState(position = location),
                title = email,

                snippet = "Shared Location"

            )

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
