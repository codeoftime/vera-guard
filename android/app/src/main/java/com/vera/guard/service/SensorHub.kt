package com.vera.guard.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SensorStatus(
    val running: Boolean = false,
    val hasAccel: Boolean = true,
    val hasGyro: Boolean = true,
    val g: Float = 1f,            // acceleration magnitude in g (1.0 = resting)
    val ax: Float = 0f, val ay: Float = 0f,   // gravity components in g, used for the level bubble
    val gyroDps: Float = 0f,      // rotation speed, degrees per second
    val detector: String = "IDLE",
    val lastSampleMs: Long = 0L,
    val shake: Float = 0f,        // 0..1 progress of the shake-to-SOS gesture
    val lastTriggerMs: Long = 0L, // elapsedRealtime of the last motion-triggered SOS
)

/** Live sensor readings published by GuardService (throttled) so the UI can show that motion sensing really works. */
@Singleton
class SensorHub @Inject constructor() {
    private val _status = MutableStateFlow(SensorStatus())
    val status: StateFlow<SensorStatus> = _status.asStateFlow()
    fun update(f: (SensorStatus) -> SensorStatus) { _status.value = f(_status.value) }
}
