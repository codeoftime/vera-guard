package com.vera.guard.detect

import com.vera.guard.detect.DetectionConfig as C

enum class Severity { LOW, MEDIUM, HIGH }

data class SeverityResult(val score: Int, val level: Severity, val countdownSeconds: Int)

object SeverityScorer {
    private fun part(v: Float, min: Float, range: Float, weight: Float) = (((v - min) / range).coerceIn(0f, 1f)) * weight

    fun score(m: FallMetrics): SeverityResult {
        val total = part(m.peakG, C.PEAK_MIN_G, C.PEAK_RANGE_G, C.W_PEAK) +
            part(m.jerkGps, 0f, C.JERK_RANGE_GPS, C.W_JERK) +
            part(m.gyroPeak, 0f, C.GYRO_RANGE, C.W_GYRO) +
            part(m.freeFallMs.toFloat(), 0f, C.FREEFALL_RANGE_MS, C.W_FREEFALL) +
            part(m.stillnessMs.toFloat(), 0f, C.STILL_RANGE_MS, C.W_STILL) +
            part(m.orientationDeg, 0f, C.ORIENT_RANGE_DEG, C.W_ORIENT) +
            part(m.impactMs.toFloat(), 0f, C.IMPACT_RANGE_MS, C.W_IMPACT)
        val score = Math.round(total).coerceIn(0, 100)
        val level = levelOf(score)
        return SeverityResult(score, level, countdownFor(level))
    }

    fun levelOf(score: Int) = when {
        score < C.LOW_MAX -> Severity.LOW
        score <= C.MEDIUM_MAX -> Severity.MEDIUM
        else -> Severity.HIGH
    }

    fun countdownFor(level: Severity) = when (level) {
        Severity.LOW -> C.COUNTDOWN_LOW_S
        Severity.MEDIUM -> C.COUNTDOWN_MEDIUM_S
        Severity.HIGH -> C.COUNTDOWN_HIGH_S
    }
}
