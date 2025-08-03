package com.example.indytrail.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import kotlin.math.*

/** Utility math helpers */
private fun median(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
}

private fun normalizeAngle(deg: Float): Float {
    var d = deg % 360f
    if (d < 0) d += 360f
    return d
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfinderScreen(
    onBack: () -> Unit,
) {
    ImmersiveSystemBars()

    val context = LocalContext.current
    val fusedAvailable = remember {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
    val fusedClient = remember { if (fusedAvailable) LocationServices.getFusedLocationProviderClient(context) else null }
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
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var warmUpCount by remember { mutableStateOf(0) }
    val buffer = remember { mutableStateListOf<Location>() }
    var smoothed by remember { mutableStateOf<Location?>(null) }
    var prevSmoothed by remember { mutableStateOf<Location?>(null) }
    var currAccuracy by remember { mutableStateOf<Float?>(null) }

    var target by remember { mutableStateOf<Location?>(null) }
    var anchorAccuracy by remember { mutableStateOf<Float?>(null) }
    var anchorTimestamp by remember { mutableStateOf<Long?>(null) }

    var sampling by remember { mutableStateOf(false) }
    val samplingFixes = remember { mutableStateListOf<Location>() }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Compass handling
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var deviceAzimuth by remember { mutableStateOf(0f) }
    var lastRawAzimuth by remember { mutableStateOf(0f) }

    fun handleLocation(loc: Location) {
        if (loc.accuracy > PathfinderParams.ACCURACY_REJECT) return
        if (System.currentTimeMillis() - loc.time > 3000) return
        if (warmUpCount < PathfinderParams.WARM_UP_COUNT) {
            warmUpCount++
            return
        }
        val medLat = median(buffer.map { it.latitude })
        val medLng = median(buffer.map { it.longitude })
        val medianLoc = Location("").apply { latitude = medLat; longitude = medLng }
        if (buffer.isNotEmpty()) {
            val dist = loc.distanceTo(medianLoc)
            if (dist > 1.5f * loc.accuracy) return
        }
        if (buffer.size == 5) buffer.removeAt(0)
        buffer.add(loc)
        val newMedLat = median(buffer.map { it.latitude })
        val newMedLng = median(buffer.map { it.longitude })
        val hold = loc.speed < PathfinderParams.STATIONARY_SPEED &&
            prevSmoothed != null &&
            smoothed != null &&
            smoothed!!.distanceTo(prevSmoothed!!) < 0.8f
        val alpha = if (hold) 0.05f else PathfinderParams.EMA_ALPHA_POSITION
        val newLat = alpha * newMedLat + (1 - alpha) * (smoothed?.latitude ?: newMedLat)
        val newLng = alpha * newMedLng + (1 - alpha) * (smoothed?.longitude ?: newMedLng)
        prevSmoothed = smoothed
        smoothed = Location("").apply {
            latitude = newLat
            longitude = newLng
            speed = loc.speed
            altitude = loc.altitude
        }
        currAccuracy = loc.accuracy
        if (sampling) samplingFixes.add(Location(loc))
    }

    DisposableEffect(hasPermission, fusedAvailable) {
        if (hasPermission) {
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach { handleLocation(it) }
                }
            }
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500)
                .setMinUpdateDistanceMeters(0f)
                .setMaxUpdateDelayMillis(0)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .build()
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    handleLocation(location)
                }
            }
            try {
                if (fusedAvailable) {
                    fusedClient?.requestLocationUpdates(request, callback, Looper.getMainLooper())
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
                }
            } catch (e: SecurityException) {
                errorMessage = "Location permission denied."
            }
            onDispose {
                if (fusedAvailable) {
                    fusedClient?.removeLocationUpdates(callback)
                } else {
                    locationManager.removeUpdates(listener)
                }
            }
        } else onDispose {}
    }

    DisposableEffect(Unit) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(matrix, orientation)
                var az = Math.toDegrees(orientation[0].toDouble()).toFloat()
                smoothed?.let {
                    val field = GeomagneticField(
                        it.latitude.toFloat(),
                        it.longitude.toFloat(),
                        it.altitude.toFloat(),
                        System.currentTimeMillis()
                    )
                    az += field.declination
                }
                az = normalizeAngle(az)
                if (abs(az - lastRawAzimuth) > PathfinderParams.ARROW_MIN_DELTA_DEG) {
                    deviceAzimuth = normalizeAngle(
                        deviceAzimuth * (1 - PathfinderParams.EMA_ALPHA_AZIMUTH) + az * PathfinderParams.EMA_ALPHA_AZIMUTH
                    )
                    lastRawAzimuth = az
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensor?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    // Timer for sampling anchor
    LaunchedEffect(sampling) {
        if (sampling) {
            samplingFixes.clear()
            var duration = PathfinderParams.ANCHOR_SAMPLE_SECONDS * 1000L
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < duration || samplingFixes.size < 5) {
                if (System.currentTimeMillis() - start >= duration && samplingFixes.size < 5) {
                    duration += 2000L
                }
                kotlinx.coroutines.delay(100)
            }
            val anchor = computeAnchor(samplingFixes)
            target = anchor.first
            anchorAccuracy = anchor.second
            anchorTimestamp = System.currentTimeMillis()
            sampling = false
        }
    }

    val distanceInfo = remember(smoothed, target) {
        val current = smoothed
        val tgt = target
        if (current != null && tgt != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                current.latitude,
                current.longitude,
                tgt.latitude,
                tgt.longitude,
                results
            )
            results[0]
        } else null
    }

    val expectedError = remember(currAccuracy, anchorAccuracy) {
        val c = currAccuracy
        val a = anchorAccuracy
        if (c != null && a != null) sqrt(c * c + a * a) else null
    }

    val arrived = remember(distanceInfo, expectedError) {
        val d = distanceInfo
        val e = expectedError ?: PathfinderParams.ARRIVED_BASE
        d != null && d <= max(PathfinderParams.ARRIVED_BASE, e)
    }

    val quantized = distanceInfo?.let { quantizeDistance(it) }

    val bearingToTarget = remember(smoothed, target) {
        val current = smoothed
        val tgt = target
        if (current != null && tgt != null) current.bearingTo(tgt) else null
    }

    // Heading mix
    val gpsCourse = remember(prevSmoothed, smoothed) {
        val prev = prevSmoothed
        val curr = smoothed
        if (prev != null && curr != null) {
            val dist = prev.distanceTo(curr)
            if (curr.speed > PathfinderParams.GPS_COURSE_SPEED && dist > 3) prev.bearingTo(curr) else null
        } else null
    }
    val heading = remember(deviceAzimuth, gpsCourse, smoothed) {
        val speed = smoothed?.speed ?: 0f
        val w = ((speed - 0.5f) / 1.5f).coerceIn(0f, 1f)
        normalizeAngle(((1 - w) * deviceAzimuth + w * (gpsCourse ?: deviceAzimuth)))
    }
    val arrow = remember(bearingToTarget, heading) {
        if (bearingToTarget != null) normalizeAngle(bearingToTarget - heading) else 0f
    }
    val arrowAlpha = remember(currAccuracy) { if ((currAccuracy ?: 0f) > 25f) 0.4f else 1f }

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
            if (target == null) {
                if (sampling) {
                    Text("Fixing...")
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                } else {
                    Text("No target location set.")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (smoothed != null) sampling = true else errorMessage = "Current location unavailable."
                        },
                        enabled = errorMessage == null
                    ) { Text("Save Current Location") }
                }
            } else {
                Text("Target: ${"%.5f".format(target!!.latitude)}, ${"%.5f".format(target!!.longitude)}")
                Spacer(Modifier.height(8.dp))
                smoothed?.let { curr ->
                    Text("Current: ${"%.5f".format(curr.latitude)}, ${"%.5f".format(curr.longitude)}")
                    Spacer(Modifier.height(8.dp))
                    if (warmUpCount < PathfinderParams.WARM_UP_COUNT) {
                        Text("Distance: …")
                    } else {
                        quantized?.let { d -> Text("Distance: $d m") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Navigation,
                            contentDescription = "Bearing",
                            modifier = Modifier.rotate(arrow).alpha(arrowAlpha)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${arrow.roundToInt()}°")
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Circle,
                            contentDescription = null,
                            tint = when {
                                (currAccuracy ?: 100f) <= 8f -> Color.Green
                                (currAccuracy ?: 100f) <= 20f -> Color.Yellow
                                else -> Color.Red.copy(alpha = 0.4f)
                            }
                        )
                    }
                } ?: Text("Waiting for location…")
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    target = null
                    anchorAccuracy = null
                    anchorTimestamp = null
                    buffer.clear()
                    smoothed = null
                    prevSmoothed = null
                    warmUpCount = 0
                }) { Text("Reset") }
                if (arrived) {
                    Spacer(Modifier.height(16.dp))
                    Text("Arrived")
                }
            }
        }
    }
}

private fun computeAnchor(fixes: List<Location>): Pair<Location, Float> {
    if (fixes.size < 5) {
        val best = fixes.minByOrNull { it.accuracy } ?: throw IllegalArgumentException("No fixes")
        return Location(best).let { it to it.accuracy }
    }
    val weights = fixes.map { 1.0 / (it.accuracy.toDouble().pow(2.0)) }
    val sumW = weights.sum()
    val lat = fixes.indices.sumOf { weights[it] * fixes[it].latitude } / sumW
    val lng = fixes.indices.sumOf { weights[it] * fixes[it].longitude } / sumW
    val acc = sqrt(1.0 / sumW).toFloat()
    val loc = Location("").apply { latitude = lat; longitude = lng }
    return loc to acc
}

private fun quantizeDistance(d: Float): Int {
    return when {
        d < 12f -> listOf(0, 3, 6, 9, 12).minByOrNull { abs(it - d) } ?: d.roundToInt()
        d <= 50f -> d.roundToInt()
        else -> (round(d / 5) * 5).toInt()
    }
}
