package com.example.indytrail.pathfinder

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.indytrail.pathfinder.model.PermissionState
import com.example.indytrail.pathfinder.model.SignalQuality
import com.example.indytrail.pathfinder.PathfinderConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfinderScreen(
    onBack: () -> Unit,
    viewModel: PathfinderViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.updatePermission(granted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermission(granted)
    }

    LaunchedEffect(state.permission) {
        if (state.permission == PermissionState.GRANTED) {
            viewModel.start()
            viewModel.checkLocationEnabled()
        } else {
            viewModel.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PATHFINDER PROTOCOL") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (state.permission) {
                PermissionState.DENIED, PermissionState.UNKNOWN -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Location permission required")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                            Text("Grant permission")
                        }
                    }
                }
                PermissionState.GRANTED -> {
                    val error = state.errorMessage
                    if (error != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                context.startActivity(intent)
                            }) { Text("Open Settings") }
                        }
                    } else if (state.warmUpActive && state.current == null) {
                        Text("Acquiring position…")
                    } else {
                        val anchor = state.anchor
                        if (anchor == null) {
                            if (state.savingAnchor) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(8.dp))
                                    Text("Fixing…")
                                }
                            } else {
                                Button(onClick = { viewModel.saveAnchor() }, enabled = state.current != null) {
                                    Text("Save Current Location")
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Target: ${anchor.latitude.format()} , ${anchor.longitude.format()}")
                                val current = state.current
                                Text("Current: ${current?.latitude?.format()} , ${current?.longitude?.format()}")
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val qColor = when (state.signalQuality) {
                                        SignalQuality.GOOD -> MaterialTheme.colorScheme.primary
                                        SignalQuality.MEDIUM -> MaterialTheme.colorScheme.tertiary
                                        SignalQuality.WEAK -> MaterialTheme.colorScheme.error
                                    }
                                    Box(modifier = Modifier.size(8.dp).background(qColor, shape = CircleShape))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Distance: ${state.distanceM?.toInt() ?: 0} m")
                                }
                                Spacer(Modifier.height(16.dp))
                                Icon(
                                    Icons.Filled.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp).rotate(state.arrowDeg ?: 0f),
                                    tint = MaterialTheme.colorScheme.primary.copy(
                                        alpha = if ((state.currentAccuracy ?: 0f) > PathfinderConfig.WEAK_SIGNAL_ACCURACY_M) 0.4f else 1f
                                    )
                                )
                                if (state.arrived) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Arrived", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.reset() }) { Text("Reset") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Double.format(): String = String.format("%.5f", this)
