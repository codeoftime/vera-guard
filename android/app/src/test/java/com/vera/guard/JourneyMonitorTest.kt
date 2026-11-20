package com.vera.guard

import com.vera.guard.journey.Geo
import com.vera.guard.journey.JourneyEvent
import com.vera.guard.journey.JourneyMonitor
import com.vera.guard.journey.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyMonitorTest {
    private val route = listOf(LatLng(12.9700, 77.5900), LatLng(12.9700, 77.6000))
    private val dest = LatLng(12.9700, 77.6000)
    private val onRoute = LatLng(12.9701, 77.5950)
    private val offRoute = LatLng(12.9740, 77.5950) // ~445 m north
    private fun monitor(eta: Long = 1_000_000_000L, buffer: Long = 0, safe: List<LatLng> = emptyList()) = JourneyMonitor(route, dest, eta, buffer, safe)

    @Test fun distances() {
        assertTrue(Geo.distanceToRoute(onRoute, route) < 15)
        assertTrue(Geo.distanceToRoute(offRoute, route) in 400.0..500.0)
        assertEquals(1084.0, Geo.haversine(route[0], route[1]), 15.0)
    }
    @Test fun deviationNeedsMoreThan60Seconds() {
        val m = monitor()
        assertTrue(m.onLocation(offRoute, 0).isEmpty())
        assertTrue(m.onLocation(offRoute, 30_000).isEmpty())
        assertEquals(listOf(JourneyEvent.DEVIATED), m.onLocation(offRoute, 61_000))
        assertTrue(m.onLocation(offRoute, 70_000).isEmpty()) // fires once per episode
        assertTrue(m.onLocation(onRoute, 80_000).isEmpty() && !m.deviating)
    }
    @Test fun shortExcursionIsIgnored() {
        val m = monitor()
        m.onLocation(offRoute, 0); m.onLocation(onRoute, 30_000)
        assertTrue(m.onLocation(offRoute, 40_000).isEmpty()); assertTrue(m.onLocation(offRoute, 90_000).isEmpty())
    }
    @Test fun unexpectedStopAfterThreeMinutes() {
        val m = monitor()
        assertTrue(m.onLocation(onRoute, 0).isEmpty())
        assertTrue(m.onLocation(onRoute, 179_000).isEmpty())
        assertEquals(listOf(JourneyEvent.STOPPED), m.onLocation(onRoute, 181_000))
    }
    @Test fun noStopAlertAtSafePlace() {
        val m = monitor(safe = listOf(LatLng(12.9702, 77.5951)))
        m.onLocation(onRoute, 0)
        assertTrue(m.onLocation(onRoute, 400_000).isEmpty())
    }
    @Test fun movingResetsStopTimer() {
        val m = monitor()
        m.onLocation(onRoute, 0); m.onLocation(LatLng(12.9701, 77.5960), 150_000)
        assertTrue(m.onLocation(LatLng(12.9701, 77.5960), 320_000).isEmpty())
    }
    @Test fun delayedAfterEtaPlusBufferAndExtendRearms() {
        val m = monitor(eta = 100_000, buffer = 50_000)
        assertTrue(m.onTick(140_000).isEmpty())
        assertEquals(listOf(JourneyEvent.DELAYED), m.onTick(151_000))
        assertTrue(m.onTick(160_000).isEmpty())
        m.extend(600_000)
        assertTrue(m.onTick(300_000).isEmpty())
        assertEquals(listOf(JourneyEvent.DELAYED), m.onTick(800_000))
    }
    @Test fun arrivalWithin100Metres() {
        val m = monitor()
        assertEquals(listOf(JourneyEvent.ARRIVED), m.onLocation(LatLng(12.9700, 77.5995), 1000)) // ~54 m
        assertTrue(m.isArrived)
        assertTrue(m.onLocation(offRoute, 500_000).isEmpty())
    }
    @Test fun notArrivedAt217Metres() = assertTrue(JourneyEvent.ARRIVED !in monitor().onLocation(LatLng(12.9700, 77.5980), 1000))
    @Test fun lowBatteryReportedOnce() {
        val m = monitor()
        assertTrue(JourneyEvent.LOW_BATTERY in m.onLocation(onRoute, 0, battery = 14))
        assertTrue(JourneyEvent.LOW_BATTERY !in m.onLocation(onRoute, 1000, battery = 13))
        assertTrue(JourneyEvent.LOW_BATTERY !in monitor().onLocation(onRoute, 0, battery = 40))
    }
}
