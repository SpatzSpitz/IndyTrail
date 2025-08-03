package com.example.indytrail.pathfinder.model

import com.example.indytrail.pathfinder.util.LatLng

enum class PermissionState { UNKNOWN, GRANTED, DENIED }

enum class SignalQuality { GOOD, MEDIUM, WEAK }

data class PathfinderState(
    val permission: PermissionState = PermissionState.UNKNOWN,
    val warmUpActive: Boolean = true,
    val anchor: LatLng? = null,
    val anchorAccuracy: Float? = null,
    val current: LatLng? = null,
    val currentAccuracy: Float? = null,
    val speed: Float? = null,
    val distanceM: Float? = null,
    val arrived: Boolean = false,
    val arrowDeg: Float? = null,
    val signalQuality: SignalQuality = SignalQuality.WEAK,
    val savingAnchor: Boolean = false,
    val errorMessage: String? = null
)
