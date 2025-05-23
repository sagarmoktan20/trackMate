package com.example.trackmate1

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.trackmate1.screens.ProfileScreen
import com.example.trackmate1.screens.SearchScreen
import com.example.trackmate1.ui.theme.Trackmate1Theme
import com.example.trackmatebackup.Screens.MapScreen


class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
        
        } else {

        }
    }

//    private val requestCode = 101
//    val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Trackmate1Theme  {
                MainScreen()


            }
        }
    askForPermission()
    }


    private fun askForPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

}


@Composable
fun MainScreen(){
    val navController = rememberNavController();
    val selected = remember {
        mutableStateOf(Icons.Default.Search)
    }

    Scaffold(
        bottomBar = {
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
    ) {paddingValues ->
        NavHost(navController = navController,
            startDestination = NavigationItems.Search.route,modifier = Modifier.padding(paddingValues)){
            composable(NavigationItems.Search.route){ SearchScreen() }
            composable(NavigationItems.Map.route){ MapScreen() }
            composable(NavigationItems.Profile.route){ ProfileScreen() }
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