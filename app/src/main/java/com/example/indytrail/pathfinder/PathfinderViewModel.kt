package com.example.indytrail.pathfinder

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.indytrail.pathfinder.location.LocationRepository
import com.example.indytrail.pathfinder.model.PathfinderState
import com.example.indytrail.pathfinder.model.PermissionState
import com.example.indytrail.pathfinder.model.SignalQuality
import com.example.indytrail.pathfinder.sensors.HeadingRepository
import com.example.indytrail.pathfinder.util.GeoUtils
import com.example.indytrail.pathfinder.util.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sqrt

class PathfinderViewModel(app: Application) : AndroidViewModel(app) {

    private val locationRepo = LocationRepository(app)
    private val headingRepo = HeadingRepository(app)

    private val _state = MutableStateFlow(PathfinderState())
    val state: StateFlow<PathfinderState> = _state.asStateFlow()

    private var locationJob: Job? = null
    private var headingJob: Job? = null
    private var sharedFlow = locationRepo.locationFlow()

    private var latestSample: LocationRepository.Sample? = null
    private var latestHeading: Float? = null

    fun updatePermission(granted: Boolean) {
        _state.value = _state.value.copy(permission = if (granted) PermissionState.GRANTED else PermissionState.DENIED)
    }

    fun start() {
        if (locationJob != null) return

        locationJob = viewModelScope.launch {
            sharedFlow.collect { sample ->
                latestSample = sample
                recalc()
            }
        }
        headingJob = viewModelScope.launch {
            headingRepo.headingFlow(sharedFlow).collect { heading ->
                latestHeading = heading
                recalc()
            }
        }
    }

    fun stop() {
        locationJob?.cancel(); locationJob = null
        headingJob?.cancel(); headingJob = null
    }

    private fun recalc() {
        val sample = latestSample ?: return
        var arrow: Float? = null
        var distance: Float? = null
        var arrived = false
        val anchor = _state.value.anchor
        val anchorAcc = _state.value.anchorAccuracy ?: 0f
        if (anchor != null) {
            val distRaw = GeoUtils.distanceMeters(sample.latLng, anchor)
            distance = quantize(distRaw)
            val expectedError = sqrt(sample.accuracy * sample.accuracy + anchorAcc * anchorAcc)
            arrived = distRaw <= max(PathfinderConfig.ARRIVED_BASE_M, expectedError)
            val head = latestHeading
            if (head != null) {
                val targetBearing = GeoUtils.bearingDegrees(sample.latLng, anchor)
                arrow = GeoUtils.normalize(targetBearing - head)
            }
        }
        val signal = when {
            sample.accuracy <= 8f -> SignalQuality.GOOD
            sample.accuracy <= 20f -> SignalQuality.MEDIUM
            else -> SignalQuality.WEAK
        }
        _state.value = _state.value.copy(
            warmUpActive = false,
            current = sample.latLng,
            currentAccuracy = sample.accuracy,
            speed = sample.speed,
            distanceM = distance,
            arrived = arrived,
            arrowDeg = arrow,
            signalQuality = signal
        )
    }

    private fun quantize(distance: Float): Float {
        return when {
            distance < 12f -> listOf(0f, 3f, 6f, 9f, 12f).minBy { kotlin.math.abs(it - distance) }
            distance <= 50f -> round(distance)
            else -> round(distance / 5f) * 5f
        }
    }

    fun saveAnchor() {
        if (_state.value.savingAnchor) return
        _state.value = _state.value.copy(savingAnchor = true)
        viewModelScope.launch {
            val samples = withTimeoutOrNull((PathfinderConfig.ANCHOR_SAMPLE_SECONDS + 2) * 1000) {
                val list = mutableListOf<LocationRepository.Sample>()
                sharedFlow.collect { s ->
                    list.add(s)
                    if (list.size >= PathfinderConfig.ANCHOR_MIN_SAMPLES) {
                        cancel()
                    }
                }
                list
            } ?: emptyList()

            if (samples.isNotEmpty()) {
                val anchor: LatLng
                val acc: Float
                if (samples.size >= PathfinderConfig.ANCHOR_MIN_SAMPLES) {
                    val weights = samples.map { 1f / (it.accuracy * it.accuracy) }
                    val sumW = weights.sum()
                    val lat = samples.indices.sumOf { weights[it] * samples[it].latLng.latitude } / sumW
                    val lng = samples.indices.sumOf { weights[it] * samples[it].latLng.longitude } / sumW
                    anchor = LatLng(lat, lng)
                    acc = sqrt(1f / sumW)
                } else {
                    val best = samples.minBy { it.accuracy }
                    anchor = best.latLng
                    acc = best.accuracy
                }
                _state.value = _state.value.copy(
                    anchor = anchor,
                    anchorAccuracy = acc,
                    savingAnchor = false
                )
                recalc()
            } else {
                _state.value = _state.value.copy(savingAnchor = false)
            }
        }
    }

    fun reset() {
        _state.value = _state.value.copy(
            anchor = null,
            anchorAccuracy = null,
            distanceM = null,
            arrived = false,
            arrowDeg = null
        )
    }

    fun checkLocationEnabled() {
        val lm = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) { true }
        if (!enabled) {
            _state.value = _state.value.copy(errorMessage = "Location is off")
        } else {
            _state.value = _state.value.copy(errorMessage = null)
        }
    }
}
