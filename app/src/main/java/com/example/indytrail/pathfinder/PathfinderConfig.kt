package com.example.indytrail.pathfinder

object PathfinderConfig {
    const val WARM_UP_COUNT = 3
    const val ACCURACY_REJECT_M = 20f
    const val REQUEST_INTERVAL_MS = 1000L
    const val REQUEST_MIN_INTERVAL_MS = 500L
    const val ANCHOR_SAMPLE_SECONDS = 3L
    const val ANCHOR_MIN_SAMPLES = 7
    const val EMA_ALPHA_POSITION = 0.15f
    const val EMA_ALPHA_AZIMUTH = 0.20f
    const val STATIONARY_SPEED_MPS = 0.3f
    const val ARRIVED_BASE_M = 6f
    const val GPS_COURSE_SPEED_MPS = 2.0f
    const val ARROW_MIN_DELTA_DEG = 2f
    const val WEAK_SIGNAL_ACCURACY_M = 25f
}

