package com.vera.guard.detect

/** All detection thresholds live here (spec 1.2). Values are overridable only by editing this file / Settings scale factors. */
object DetectionConfig {
    const val G = 9.80665f

    // FREE_FALL
    const val FREE_FALL_G = 0.5f            // magnitude below this ...
    const val FREE_FALL_MIN_MS = 100L       // ... for at least this long
    // IMPACT
    const val IMPACT_G = 2.5f               // spike after free fall
    const val IMPACT_WINDOW_MS = 1000L      // must follow free fall within 1 s
    const val CRASH_IMPACT_G = 6.0f         // impact without free fall (vehicle crash): higher threshold
    const val IMPACT_DURATION_G = 1.8f      // samples above this count towards impact duration
    const val IMPACT_COLLECT_MS = 300L
    const val MIN_IMPACT_MS = 40L           // >= 2 samples above IMPACT_DURATION_G (filters a phone hitting a table)
    const val SAMPLE_PERIOD_GUESS_MS = 20L
    // STILLNESS_CHECK
    const val SETTLE_MS = 500L
    const val STILLNESS_MIN_MS = 3000L      // 3..5 s window
    const val STILL_DEV_G = 0.20f           // | |a|-1g | below this = still
    const val STILL_GYRO_RAD_S = 0.6f
    const val STILL_MAX_VIOLATIONS = 2
    const val ORIENT_MIN_DEG = 30f          // gravity vector must have rotated (fall path)
    // Cancel by deliberate pick-up and walking
    const val WALK_PEAK_G = 1.3f
    const val WALK_STEPS = 5
    const val WALK_WINDOW_MS = 5000L
    const val WALK_MIN_INTERVAL_MS = 250L
    const val WALK_MAX_INTERVAL_MS = 1100L
    // Ring buffer
    const val BUFFER_MS = 5000L

    // Shake gesture: SHAKE_COUNT hard shakes (each = two strokes) within SHAKE_WINDOW_MS
    const val SHAKE_COUNT = 3
    const val SHAKE_WINDOW_MS = 3500L
    const val SHAKE_STROKE_MS2 = 13.0f      // m/s^2 of high-passed acceleration per stroke (~1.3 g)
    const val SHAKE_MIN_GAP_MS = 80L
    const val SHAKE_MAX_GAP_MS = 700L

    // Severity weights (sum = 100) and scales
    const val W_PEAK = 30f;  const val PEAK_MIN_G = 2f;  const val PEAK_RANGE_G = 5f
    const val W_JERK = 10f;  const val JERK_RANGE_GPS = 300f
    const val W_GYRO = 15f;  const val GYRO_RANGE = 10f
    const val W_FREEFALL = 10f; const val FREEFALL_RANGE_MS = 600f
    const val W_STILL = 20f; const val STILL_RANGE_MS = 6000f
    const val W_ORIENT = 10f; const val ORIENT_RANGE_DEG = 90f
    const val W_IMPACT = 5f; const val IMPACT_RANGE_MS = 200f
    const val LOW_MAX = 35
    const val MEDIUM_MAX = 70

    // Countdown (seconds) before escalation
    const val COUNTDOWN_LOW_S = 30
    const val COUNTDOWN_MEDIUM_S = 20
    const val COUNTDOWN_HIGH_S = 10
}
