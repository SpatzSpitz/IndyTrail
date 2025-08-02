package com.example.indytrail.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfinderScreen(
    onBack: () -> Unit
) {
    ImmersiveSystemBars()

    val context = LocalContext.current
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var targetLocation by remember { mutableStateOf<Location?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                errorMessage = "Location services unavailable."
            }
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    currentLocation = location
                }
            }
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    listener
                )
            } catch (e: SecurityException) {
                errorMessage = "Location permission denied."
            }
            onDispose {
                locationManager.removeUpdates(listener)
            }
        } else {
            onDispose {}
        }
    }

    val distance = remember(currentLocation, targetLocation) {
        if (currentLocation != null && targetLocation != null) {
            currentLocation!!.distanceTo(targetLocation).roundToInt()
        } else null
    }
    val bearing = remember(currentLocation, targetLocation) {
        if (currentLocation != null && targetLocation != null) {
            currentLocation!!.bearingTo(targetLocation)
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PATHFINDER PROTOCOL") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!hasPermission) {
                Text("Location permission required.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                    Text("Grant Permission")
                }
                return@Column
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
            }

            if (targetLocation == null) {
                Text("No target location set.")
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        currentLocation?.let { loc ->
                            targetLocation = Location(loc)
                        } ?: run {
                            errorMessage = "Current location unavailable."
                        }
                    },
                    enabled = errorMessage == null
                ) {
                    Text("Save Current Location")
                }
            } else {
                Text("Target: ${targetLocation!!.latitude}, ${targetLocation!!.longitude}")
                Spacer(Modifier.height(8.dp))
                currentLocation?.let {
                    Text("Current: ${it.latitude}, ${it.longitude}")
                    Spacer(Modifier.height(8.dp))
                    distance?.let { d ->
                        Text("Distance: $d m")
                        Spacer(Modifier.height(8.dp))
                    }
                    bearing?.let { b ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Navigation,
                                contentDescription = "Bearing",
                                modifier = Modifier.rotate(b)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${b.roundToInt()}°")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                } ?: Text("Waiting for location...")
                Spacer(Modifier.height(24.dp))
                Button(onClick = { targetLocation = null }) {
                    Text("Reset")
                }
            }
        }
    }
}
