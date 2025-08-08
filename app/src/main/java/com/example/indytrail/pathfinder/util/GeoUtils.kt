package com.example.indytrail.pathfinder.util

import android.location.Location
import kotlin.math.*

data class LatLng(val latitude: Double, val longitude: Double)

object GeoUtils {
    fun distanceMeters(a: LatLng, b: LatLng): Float {
        val res = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, res)
        return res[0]
    }

    fun bearingDegrees(a: LatLng, b: LatLng): Float {
        val res = FloatArray(2)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, res)
        return res[1]
    }

    fun normalize(deg: Float): Float {
        var d = deg % 360f
        if (d < 0) d += 360f
        return d
    }
}
