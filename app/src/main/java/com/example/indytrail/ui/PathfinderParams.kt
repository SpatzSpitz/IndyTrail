package com.example.indytrail.ui

object PathfinderParams {
    const val WARM_UP_COUNT = 3
    const val ACCURACY_REJECT = 20f
    const val ANCHOR_SAMPLE_SECONDS = 3L
    const val ANCHOR_MIN_SAMPLES = 7
    const val EMA_ALPHA_POSITION = 0.15f
    const val EMA_ALPHA_AZIMUTH = 0.2f
    const val STATIONARY_SPEED = 0.3f
    const val ARRIVED_BASE = 6f
    const val GPS_COURSE_SPEED = 2.0f
    const val ARROW_MIN_DELTA_DEG = 2f
}
