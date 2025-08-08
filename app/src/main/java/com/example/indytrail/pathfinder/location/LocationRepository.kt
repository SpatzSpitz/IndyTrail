package com.example.indytrail.pathfinder.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.example.indytrail.pathfinder.PathfinderConfig
import com.example.indytrail.pathfinder.util.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.ArrayDeque

class LocationRepository(private val context: Context) {

    data class Sample(
        val latLng: LatLng,
        val accuracy: Float,
        val speed: Float,
        val hasBearing: Boolean,
        val bearing: Float,
        val bearingAccuracy: Float?,
        val time: Long
    )

    private var client: FusedLocationProviderClient? = null
    private var manager: LocationManager? = null

    init {
        client = try { LocationServices.getFusedLocationProviderClient(context) } catch (_: Exception) { null }
        manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @SuppressLint("MissingPermission")
    fun locationFlow(): Flow<Sample> = callbackFlow {
        var warmup = 0
        val buffer = ArrayDeque<Location>()
        var emaLat: Double? = null
        var emaLng: Double? = null

        fun handle(loc: Location) {
            if (loc.accuracy > PathfinderConfig.ACCURACY_REJECT_M) return
            if (System.currentTimeMillis() - loc.time > 3000) return
            if (warmup < PathfinderConfig.WARM_UP_COUNT) { warmup++; return }

            buffer.addLast(loc)
            if (buffer.size > 5) buffer.removeFirst()
            val medianLat = buffer.map { it.latitude }.sorted()[buffer.size/2]
            val medianLng = buffer.map { it.longitude }.sorted()[buffer.size/2]
            val d = FloatArray(1)
            Location.distanceBetween(loc.latitude, loc.longitude, medianLat, medianLng, d)
            if (d[0] > 1.5f * loc.accuracy) return

            if (emaLat == null) {
                emaLat = loc.latitude
                emaLng = loc.longitude
            } else {
                emaLat = emaLat!! + PathfinderConfig.EMA_ALPHA_POSITION * (loc.latitude - emaLat!!)
                emaLng = emaLng!! + PathfinderConfig.EMA_ALPHA_POSITION * (loc.longitude - emaLng!!)
            }
            val smoothed = LatLng(emaLat!!, emaLng!!)
            trySend(
                Sample(
                    latLng = smoothed,
                    accuracy = loc.accuracy,
                    speed = loc.speed,
                    hasBearing = loc.hasBearing(),
                    bearing = loc.bearing,
                    bearingAccuracy = if (loc.hasBearingAccuracy()) loc.bearingAccuracyDegrees else null,
                    time = loc.time
                )
            )
        }

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                handle(loc)
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, PathfinderConfig.REQUEST_INTERVAL_MS)
            .setMinUpdateIntervalMillis(PathfinderConfig.REQUEST_MIN_INTERVAL_MS)
            .setMinUpdateDistanceMeters(0f)
            .build()

        val fused = client
        if (fused != null) {
            fused.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
            awaitClose { fused.removeLocationUpdates(fusedCallback) }
        } else {
            val listener = android.location.LocationListener { loc -> handle(loc) }
            manager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, PathfinderConfig.REQUEST_INTERVAL_MS, 0f, listener)
            awaitClose { manager?.removeUpdates(listener) }
        }
    }
}
