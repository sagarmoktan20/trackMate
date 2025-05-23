package com.example.trackmatebackup.Screens

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.trackmate1.ui.theme.Trackmate1Theme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import java.util.jar.Manifest

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

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties
    ) {
        Marker(
            state = rememberMarkerState(position = LatLng(27.7172, 85.3240)),
            title = "Kathmandu",
            snippet = "Capital of Nepal"
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
