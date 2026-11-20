package com.vera.guard.detect

import com.vera.guard.detect.DetectionConfig as C
import kotlin.math.abs

/**
 * Detects N hard shakes within a window. Gravity is removed with a low-pass filter; a "stroke" is a
 * high-passed excursion above SHAKE_STROKE_MS2 that alternates in sign on one axis. Running produces
 * strong positive pulses only (the negative excursion cannot exceed ~1 g), so it never alternates.
 */
class ShakeDetector {
    private val grav = FloatArray(3)
    private var primed = false
    private val strokeTimes = ArrayDeque<Long>()
    private val lastSign = IntArray(3)
    private val lastStrokeAt = LongArray(3)

    fun reset() { strokeTimes.clear(); lastSign.fill(0); lastStrokeAt.fill(0); primed = false }

    /** Returns true when the shake gesture is complete (state resets afterwards). */
    fun onSample(s: Sample): Boolean {
        val a = floatArrayOf(s.ax, s.ay, s.az)
        if (!primed) { for (i in 0..2) grav[i] = a[i]; primed = true; return false }
        for (i in 0..2) grav[i] = 0.9f * grav[i] + 0.1f * a[i]
        // The axis with the strongest high-passed excursion
        var best = 0; var bestAbs = 0f
        for (i in 0..2) { val h = abs(a[i] - grav[i]); if (h > bestAbs) { bestAbs = h; best = i } }
        if (bestAbs >= C.SHAKE_STROKE_MS2) {
            val sign = if (a[best] - grav[best] > 0) 1 else -1
            val gap = s.tMs - lastStrokeAt[best]
            if (sign != lastSign[best] && (lastStrokeAt[best] == 0L || gap >= C.SHAKE_MIN_GAP_MS)) {
                if (lastStrokeAt[best] != 0L && gap > C.SHAKE_MAX_GAP_MS) strokeTimes.clear()
                lastSign[best] = sign; lastStrokeAt[best] = s.tMs
                strokeTimes.addLast(s.tMs)
            }
        }
        while (strokeTimes.isNotEmpty() && s.tMs - strokeTimes.first() > C.SHAKE_WINDOW_MS) strokeTimes.removeFirst()
        if (strokeTimes.size >= 2 * C.SHAKE_COUNT - 1) { reset(); return true }
        return false
    }
}
