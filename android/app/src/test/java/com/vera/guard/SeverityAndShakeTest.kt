package com.vera.guard

import com.vera.guard.detect.FallMetrics
import com.vera.guard.detect.Sample
import com.vera.guard.detect.SeverityScorer
import com.vera.guard.detect.Severity
import com.vera.guard.detect.ShakeDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SeverityScorerTest {
    private fun m(peak: Float = 5f, imp: Long = 80, jerk: Float = 150f, gyro: Float = 5f, ff: Long = 400, still: Long = 3500, ori: Float = 80f) =
        FallMetrics(peak, imp, jerk, gyro, ff, still, ori, true)

    @Test fun gentleEventIsLow() {
        val r = SeverityScorer.score(m(peak = 2.7f, imp = 40, jerk = 40f, gyro = 1f, ff = 120, still = 3000, ori = 35f))
        assertEquals(Severity.LOW, r.level); assertEquals(30, r.countdownSeconds)
    }
    @Test fun violentEventIsHigh() {
        val r = SeverityScorer.score(m(peak = 9f, imp = 200, jerk = 350f, gyro = 10f, ff = 700, still = 20000, ori = 90f))
        assertEquals(Severity.HIGH, r.level); assertEquals(10, r.countdownSeconds); assertTrue(r.score in 71..100)
    }
    @Test fun typicalFallIsMedium() {
        val r = SeverityScorer.score(m())
        assertEquals(Severity.MEDIUM, r.level); assertEquals(20, r.countdownSeconds)
    }
    @Test fun scoreGrowsWithPeakAndStillness() {
        assertTrue(SeverityScorer.score(m(peak = 7f)).score > SeverityScorer.score(m(peak = 4f)).score)
        assertTrue(SeverityScorer.score(m(still = 30000)).score > SeverityScorer.score(m(still = 3000)).score)
    }
    @Test fun scoreIsClamped() {
        val r = SeverityScorer.score(m(peak = 100f, imp = 5000, jerk = 1e6f, gyro = 100f, ff = 5000, still = 1_000_000, ori = 180f))
        assertEquals(100, r.score)
        assertTrue(SeverityScorer.score(m(peak = 0f, imp = 0, jerk = 0f, gyro = 0f, ff = 0, still = 0, ori = 0f)).score >= 0)
    }
    @Test fun boundaries() {
        assertEquals(Severity.LOW, SeverityScorer.levelOf(34)); assertEquals(Severity.MEDIUM, SeverityScorer.levelOf(35))
        assertEquals(Severity.MEDIUM, SeverityScorer.levelOf(70)); assertEquals(Severity.HIGH, SeverityScorer.levelOf(71))
    }
}

class ShakeDetectorTest {
    private val G = Traces.G
    private fun shake(hz: Double, ampMs2: Float, durMs: Int) = (0 until durMs / 20).map { k ->
        Sample(k * 20L, ampMs2 * sin(k * 0.02 * 2 * PI * hz).toFloat(), G, 0f)
    }
    private fun fires(tr: List<Sample>): Boolean { val d = ShakeDetector(); var hit = false; for (s in tr) if (d.onSample(s)) hit = true; return hit }

    @Test fun fourHardShakesFire() = assertTrue(fires(shake(4.0, 35f, 1500)))
    @Test fun gentleWavingDoesNot() = assertFalse(fires(shake(2.0, 8f, 4000)))
    @Test fun walkingDoesNot() = assertFalse(fires(Traces.walking()))
    @Test fun runningDoesNot() = assertFalse(fires(Traces.running()))
    @Test fun busRideDoesNot() = assertFalse(fires(Traces.bus()))
    @Test fun slowShakesOutsideWindowDoNot() = assertFalse(fires(shake(0.7, 35f, 9000)))
}
