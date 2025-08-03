package com.example.indytrail.pathfinder.sensors

import android.content.Context
import android.hardware.*
import com.example.indytrail.pathfinder.PathfinderConfig
import com.example.indytrail.pathfinder.location.LocationRepository
import com.example.indytrail.pathfinder.util.GeoUtils
import com.example.indytrail.pathfinder.util.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class HeadingRepository(private val context: Context) {

    fun headingFlow(locations: Flow<LocationRepository.Sample>): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var lastAzimuth: Float? = null
        var currentLatLng: LatLng? = null
        var gpsCourse: Float? = null
        var lastLocation: LocationRepository.Sample? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                var az = Math.toDegrees(orientation[0].toDouble()).toFloat()
                currentLatLng?.let { ll ->
                    val field = android.hardware.GeomagneticField(
                        ll.latitude.toFloat(), ll.longitude.toFloat(), 0f, System.currentTimeMillis()
                    )
                    az += field.declination
                }
                if (lastAzimuth == null) {
                    lastAzimuth = az
                } else {
                    val diff = GeoUtils.normalize(az - lastAzimuth!!)
                    lastAzimuth = GeoUtils.normalize(lastAzimuth!! + PathfinderConfig.EMA_ALPHA_AZIMUTH * diff)
                }
                maybeEmit()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        fun maybeEmit() {
            val device = lastAzimuth ?: return
            val course = gpsCourse
            val loc = lastLocation
            if (loc == null) { trySend(device); return }
            val w = ((loc.speed - 0.5f) / 1.5f).coerceIn(0f, 1f)
            val mix = if (course != null) GeoUtils.normalize((1 - w) * device + w * course) else device
            trySend(mix)
        }

        sensorManager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_GAME)

        val locJob = launch {
            locations.collect { sample ->
                currentLatLng = sample.latLng
                if (sample.speed >= PathfinderConfig.GPS_COURSE_SPEED_MPS) {
                    lastLocation?.let { prev ->
                        val dist = GeoUtils.distanceMeters(prev.latLng, sample.latLng)
                        if (dist >= 3f) {
                            gpsCourse = GeoUtils.bearingDegrees(prev.latLng, sample.latLng)
                        }
                    }
                } else if (sample.hasBearing && (sample.bearingAccuracy ?: 999f) <= 15f) {
                    gpsCourse = sample.bearing
                }
                lastLocation = sample
                maybeEmit()
            }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
            locJob.cancel()
        }
    }
}
