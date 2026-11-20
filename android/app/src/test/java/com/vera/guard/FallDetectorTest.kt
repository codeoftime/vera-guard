package com.vera.guard

import com.vera.guard.detect.DetectorResult
import com.vera.guard.detect.DetectorState
import com.vera.guard.detect.FallDetector
import com.vera.guard.detect.Sample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Synthetic 50 Hz traces. Mirrors the traces validated in the Python prototype. */
object Traces {
    const val G = 9.80665f
    private val rnd = Random(1)
    private fun n(a: Float) = (rnd.nextFloat() * 2 - 1) * a

    fun still(t0: Long, durMs: Long, v: FloatArray, noise: Float = 0.03f): List<Sample> {
        val out = ArrayList<Sample>(); var t = t0
        while (t < t0 + durMs) { out += Sample(t, v[0] + n(noise), v[1] + n(noise), v[2] + n(noise), n(0.02f)); t += 20 }
        return out
    }
    fun last(l: List<Sample>) = l.last().tMs + 20

    fun fall(
        pre: FloatArray = floatArrayOf(0f, G, 0f), post: FloatArray = floatArrayOf(0f, 0f, G),
        ffMs: Int = 400, peaks: List<Float> = listOf(5f, 3f, 2.2f, 1.5f), postMs: Long = 6000, rot: Float = 5f,
    ): List<Sample> {
        val out = ArrayList<Sample>(still(0, 2000, pre)); var t = last(out)
        repeat(ffMs / 20) { out += Sample(t, 0.2f, 0.3f, 0.1f, rot); t += 20 }
        for (p in peaks) { out += Sample(t, p * G * 0.3f, p * G * 0.9f, p * G * 0.3f, rot); t += 20 }
        out += still(t, postMs, post)
        return out
    }

    fun walking(durMs: Int = 10000, hz: Double = 1.9): List<Sample> = (0 until durMs / 20).map { k ->
        val ph = k * 0.02 * 2 * PI * hz
        Sample(k * 20L, (0.5 * sin(ph)).toFloat(), (G + 4 * sin(ph)).toFloat(), 0.3f, (0.5 * sin(k * 0.1)).toFloat(), 0.2f)
    }

    fun running(durMs: Int = 10000): List<Sample> = (0 until durMs / 20).map { k ->
        val ph = ((k * 20) % 330) / 330.0
        val y = when { ph < 0.3 -> 0.3 * G; ph < 0.40 -> 3.5 * G; else -> G * (1.0 + 0.3 * sin(ph * 6)) }
        Sample(k * 20L, 0.5f, y.toFloat(), 0.3f, 2f, 0.5f)
    }

    fun bus(durMs: Int = 12000): List<Sample> = (0 until durMs / 20).map { k ->
        Sample(k * 20L, n(3f), G + n(6f), n(3f), n(0.3f))
    }
}

class FallDetectorTest {
    private fun run(trace: List<Sample>): Pair<List<DetectorResult>, FallDetector> {
        val d = FallDetector(); val r = ArrayList<DetectorResult>()
        for (s in trace) { val x = d.onSample(s); if (x != DetectorResult.NONE) r += x }
        return r to d
    }

    @Test fun realFallIsDetectedWithMetrics() {
        val (r, d) = run(Traces.fall())
        assertEquals(listOf(DetectorResult.DETECTED), r)
        val m = d.currentMetrics()!!
        assertTrue("peak ${m.peakG}", m.peakG in 4.5f..5.5f)
        assertTrue(m.hadFreeFall)
        assertTrue("ff ${m.freeFallMs}", m.freeFallMs in 300..450)
        assertTrue("orientation ${m.orientationDeg}", m.orientationDeg > 80f)
        assertTrue(m.stillnessMs >= 3000)
        assertEquals(DetectorState.CONFIRMING, d.state)
    }

    @Test fun vehicleCrashWithoutFreeFallIsDetected() {
        val tr = ArrayList(Traces.still(0, 2000, floatArrayOf(0f, Traces.G, 0f)))
        var t = Traces.last(tr)
        for (p in listOf(8f, 7f, 4f, 2.5f)) { tr += Sample(t, p * Traces.G * 0.5f, p * Traces.G * 0.8f, p * Traces.G * 0.3f, 3f); t += 20 }
        tr += Traces.still(t, 6000, floatArrayOf(0.2f, Traces.G, 0.1f))
        val (r, d) = run(tr)
        assertEquals(listOf(DetectorResult.DETECTED), r)
        assertTrue(!d.currentMetrics()!!.hadFreeFall)
    }

    @Test fun shortStillnessIsIgnored() = assertTrue(run(Traces.fall(postMs = 2000)).first.isEmpty())

    @Test fun personWhoGetsUpIsIgnored() {
        val tr = ArrayList(Traces.fall(postMs = 1500)); var t = Traces.last(tr)
        repeat(150) { tr += Sample(t, 6f, Traces.G + 8f, -6f, 1.5f, 1f); t += 20 }
        assertTrue(run(tr).first.isEmpty())
    }

    @Test fun phoneDroppedOnTableIsIgnored() {
        val flat = floatArrayOf(0f, 0f, Traces.G)
        assertTrue(run(Traces.fall(pre = flat, post = flat, ffMs = 400, peaks = listOf(12f, 3f))).first.isEmpty())
    }

    @Test fun sittingDownHardIsIgnored() {
        val tr = ArrayList(Traces.still(0, 2000, floatArrayOf(0f, Traces.G, 0f))); var t = Traces.last(tr)
        for (p in listOf(2.2f, 1.6f)) { tr += Sample(t, 0f, p * Traces.G, 0f); t += 20 }
        tr += Traces.still(t, 6000, floatArrayOf(0f, Traces.G, 0f))
        assertTrue(run(tr).first.isEmpty())
    }

    @Test fun walkingRunningAndBumpyBusAreIgnored() {
        assertTrue("walking", run(Traces.walking()).first.isEmpty())
        assertTrue("running", run(Traces.running()).first.isEmpty())
        assertTrue("bus", run(Traces.bus()).first.isEmpty())
    }

    @Test fun deliberatePickUpAndWalkingCancelsConfirmation() {
        val tr = ArrayList(Traces.fall(postMs = 4000)); var t = Traces.last(tr)
        repeat(300) { k -> val ph = ((k * 20) % 550) / 550.0; tr += Sample(t, 0.3f, Traces.G * (1f + if (ph < 0.2) 0.6f else 0f) + 0.1f, 0.2f, 1f); t += 20 }
        val (r, d) = run(tr)
        assertEquals(listOf(DetectorResult.DETECTED, DetectorResult.CANCELLED_BY_MOVEMENT), r)
        assertNotNull(d)
        assertEquals(DetectorState.IDLE, d.state)
    }
}
