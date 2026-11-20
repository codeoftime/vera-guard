package com.vera.guard.detect

import com.vera.guard.detect.DetectionConfig as C
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

data class Sample(val tMs: Long, val ax: Float, val ay: Float, val az: Float, val gx: Float = 0f, val gy: Float = 0f, val gz: Float = 0f) {
    /** Acceleration magnitude in g. */
    val g: Float get() = sqrt(ax * ax + ay * ay + az * az) / C.G
    val gyro: Float get() = sqrt(gx * gx + gy * gy + gz * gz)
}

enum class DetectorState { IDLE, FREE_FALL, IMPACT, STILLNESS_CHECK, CONFIRMING, ALERTING }

data class FallMetrics(
    val peakG: Float,
    val impactMs: Long,
    val jerkGps: Float,
    val gyroPeak: Float,
    val freeFallMs: Long,
    val stillnessMs: Long,
    val orientationDeg: Float,
    val hadFreeFall: Boolean,
)

enum class DetectorResult { NONE, DETECTED, CANCELLED_BY_MOVEMENT }

/**
 * Pure state machine (no Android deps) so it can be unit-tested with synthetic traces.
 * IDLE -> FREE_FALL -> IMPACT -> STILLNESS_CHECK -> CONFIRMING -> ALERTING
 */
class FallDetector(sensitivity: Float = 1f) {
    private val impactG = C.IMPACT_G / sensitivity
    private val crashG = C.CRASH_IMPACT_G / sensitivity
    var state: DetectorState = DetectorState.IDLE
        private set
    private val buf = ArrayDeque<Sample>()

    private var ffCandStart = -1L
    private var ffStart = 0L
    private var ffLastLow = 0L
    private var ffMs = 0L
    private var hadFF = false

    private var impactStart = 0L
    private var peakG = 0f
    private var firstAbove = -1L
    private var lastAbove = -1L
    private var jerkMax = 0f
    private var prevG = 1f
    private var prevT = 0L
    private var impactMs = 0L
    private var gyroPeak = 0f

    private var stillStart = 0L
    private var violations = 0
    private var stillnessMs = 0L
    private var moved = false
    private var preGravity: FloatArray? = null
    private var orientationDeg = 0f
    private val peaks = ArrayDeque<Long>()
    private var wasAbove = false

    var lastMetrics: FallMetrics? = null
        private set

    fun reset() {
        state = DetectorState.IDLE
        ffCandStart = -1; hadFF = false; ffMs = 0; firstAbove = -1; lastAbove = -1
        peakG = 0f; jerkMax = 0f; violations = 0; stillnessMs = 0; moved = false
        preGravity = null; peaks.clear(); wasAbove = false; orientationDeg = 0f
    }

    fun markAlerting() { state = DetectorState.ALERTING }

    fun currentMetrics(): FallMetrics? = lastMetrics?.copy(stillnessMs = stillnessMs)

    fun onSample(s: Sample): DetectorResult {
        buf.addLast(s)
        while (buf.isNotEmpty() && s.tMs - buf.first().tMs > C.BUFFER_MS) buf.removeFirst()
        val m = s.g
        var result = DetectorResult.NONE
        when (state) {
            DetectorState.IDLE -> {
                if (m < C.FREE_FALL_G) {
                    if (ffCandStart < 0) ffCandStart = s.tMs
                    ffLastLow = s.tMs
                    if (s.tMs - ffCandStart >= C.FREE_FALL_MIN_MS) {
                        state = DetectorState.FREE_FALL
                        ffStart = ffCandStart
                        ffMs = s.tMs - ffStart
                    }
                } else {
                    ffCandStart = -1
                }
                if (state == DetectorState.IDLE && m > crashG) beginImpact(s, m, false)
            }
            DetectorState.FREE_FALL -> {
                if (m < C.FREE_FALL_G) {
                    ffLastLow = s.tMs
                    ffMs = s.tMs - ffStart
                } else if (m > impactG) {
                    beginImpact(s, m, true)
                } else if (s.tMs - ffLastLow > C.IMPACT_WINDOW_MS) {
                    reset()
                }
            }
            DetectorState.IMPACT -> {
                collectImpact(s, m)
                if (s.tMs - impactStart >= C.IMPACT_COLLECT_MS) finishImpact(s)
            }
            DetectorState.STILLNESS_CHECK -> result = stillness(s, m)
            DetectorState.CONFIRMING -> result = confirming(s, m)
            DetectorState.ALERTING -> Unit
        }
        prevG = m; prevT = s.tMs
        return result
    }

    private fun beginImpact(s: Sample, m: Float, withFreeFall: Boolean) {
        hadFF = withFreeFall
        state = DetectorState.IMPACT
        impactStart = s.tMs
        peakG = 0f; firstAbove = -1; lastAbove = -1; jerkMax = 0f; gyroPeak = 0f
        collectImpact(s, m)
    }

    private fun collectImpact(s: Sample, m: Float) {
        if (m > peakG) peakG = m
        if (m > C.IMPACT_DURATION_G) { if (firstAbove < 0) firstAbove = s.tMs; lastAbove = s.tMs }
        val dt = (s.tMs - prevT).coerceAtLeast(1L)
        if (prevT > 0) {
            val jerk = abs(m - prevG) / (dt / 1000f)
            if (jerk > jerkMax) jerkMax = jerk
        }
        if (s.gyro > gyroPeak) gyroPeak = s.gyro
    }

    private fun finishImpact(s: Sample) {
        impactMs = if (firstAbove < 0) 0 else (lastAbove - firstAbove) + C.SAMPLE_PERIOD_GUESS_MS
        val refStart = if (hadFF) ffStart else impactStart
        // gyro rotation during the fall too
        for (x in buf) if (x.tMs >= refStart - 200 && x.gyro > gyroPeak) gyroPeak = x.gyro
        val threshold = if (hadFF) impactG else crashG
        if (peakG < threshold || impactMs < C.MIN_IMPACT_MS) { reset(); return }
        preGravity = meanVector(refStart - 1500, refStart - 300)
        stillStart = s.tMs + C.SETTLE_MS
        violations = 0; stillnessMs = 0; moved = false
        state = DetectorState.STILLNESS_CHECK
    }

    private fun isStill(s: Sample, m: Float) = abs(m - 1f) <= C.STILL_DEV_G && s.gyro <= C.STILL_GYRO_RAD_S

    private fun stillness(s: Sample, m: Float): DetectorResult {
        if (s.tMs < stillStart) return DetectorResult.NONE
        if (!isStill(s, m)) {
            violations++
            if (violations > C.STILL_MAX_VIOLATIONS) { reset(); return DetectorResult.NONE }
        }
        val elapsed = s.tMs - stillStart
        if (elapsed >= C.STILLNESS_MIN_MS) {
            val post = meanVector(stillStart, s.tMs)
            val pre = preGravity
            orientationDeg = if (pre != null && post != null) angleDeg(pre, post) else 0f
            if (hadFF && pre != null && orientationDeg < C.ORIENT_MIN_DEG) { reset(); return DetectorResult.NONE }
            stillnessMs = elapsed + C.SETTLE_MS
            lastMetrics = FallMetrics(peakG, impactMs, jerkMax, gyroPeak, ffMs, stillnessMs, orientationDeg, hadFF)
            peaks.clear(); wasAbove = false; moved = false
            state = DetectorState.CONFIRMING
            return DetectorResult.DETECTED
        }
        return DetectorResult.NONE
    }

    /** While confirming: keep measuring stillness and watch for deliberate pick-up + walking. */
    private fun confirming(s: Sample, m: Float): DetectorResult {
        if (!moved) {
            if (isStill(s, m)) stillnessMs = s.tMs - stillStart + C.SETTLE_MS else moved = true
        }
        val above = m > C.WALK_PEAK_G
        if (above && !wasAbove) {
            peaks.addLast(s.tMs)
            while (peaks.isNotEmpty() && s.tMs - peaks.first() > C.WALK_WINDOW_MS) peaks.removeFirst()
            if (peaks.size >= C.WALK_STEPS) {
                val gaps = peaks.zipWithNext { a, b -> b - a }
                if (gaps.all { it in C.WALK_MIN_INTERVAL_MS..C.WALK_MAX_INTERVAL_MS }) { reset(); return DetectorResult.CANCELLED_BY_MOVEMENT }
            }
        }
        wasAbove = above
        return DetectorResult.NONE
    }

    private fun meanVector(fromMs: Long, toMs: Long): FloatArray? {
        var n = 0; var x = 0f; var y = 0f; var z = 0f
        for (b in buf) if (b.tMs in fromMs..toMs) { x += b.ax; y += b.ay; z += b.az; n++ }
        return if (n < 3) null else floatArrayOf(x / n, y / n, z / n)
    }

    companion object {
        fun angleDeg(a: FloatArray, b: FloatArray): Float {
            val na = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]); val nb = sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2])
            if (na < 1e-3f || nb < 1e-3f) return 0f
            val c = ((a[0] * b[0] + a[1] * b[1] + a[2] * b[2]) / (na * nb)).coerceIn(-1f, 1f)
            return Math.toDegrees(acos(c.toDouble())).toFloat()
        }
    }
}
