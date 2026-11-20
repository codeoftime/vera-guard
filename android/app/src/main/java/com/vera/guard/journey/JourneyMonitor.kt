package com.vera.guard.journey

/** Journey thresholds (spec 5.2). Constants in one place. */
object JourneyConfig {
    const val DEVIATION_M = 300.0
    const val DEVIATION_SEC = 60L
    const val STOP_MIN_SEC = 180L
    const val STOP_RADIUS_M = 25.0
    const val SAFE_PLACE_RADIUS_M = 150.0
    const val ARRIVAL_M = 100.0
    const val LOW_BATTERY_PCT = 15
    const val CHECKIN_SEC = 30
    const val LOCATION_INTERVAL_MS = 10_000L
    const val WATCHDOG_SEC = 300
}

enum class JourneyEvent { DEVIATED, STOPPED, DELAYED, ARRIVED, LOW_BATTERY }

/**
 * Pure journey logic. Feed it locations and ticks; it returns events once per episode.
 * Deviation: > 300 m off route for > 60 s. Stop: stationary > 3 min, away from destination and safe places.
 * Delay: past ETA + buffer. Arrival: within 100 m of the destination (equivalent to the geofence).
 */
class JourneyMonitor(
    private val route: List<LatLng>,
    private val dest: LatLng,
    private var etaMs: Long,
    private val bufferMs: Long = 0,
    private val safePlaces: List<LatLng> = emptyList(),
) {
    private var deviatedSince = -1L
    private var deviatedLatched = false
    private var anchor: LatLng? = null
    private var anchorSince = 0L
    private var stoppedLatched = false
    private var delayedLatched = false
    private var arrived = false
    private var lowBatLatched = false

    val deviating: Boolean get() = deviatedLatched
    val stopped: Boolean get() = stoppedLatched
    val isArrived: Boolean get() = arrived
    fun etaMillis() = etaMs

    fun extend(ms: Long) { etaMs += ms; delayedLatched = false }

    /** Called when the user answers the check-in prompt "I'm OK": re-arms the triggers. */
    fun acknowledge(tMs: Long, p: LatLng?) {
        deviatedSince = -1; deviatedLatched = false; stoppedLatched = false
        anchor = p; anchorSince = tMs
    }

    fun onLocation(p: LatLng, tMs: Long, battery: Int? = null): List<JourneyEvent> {
        if (arrived) return emptyList()
        val out = ArrayList<JourneyEvent>()
        if (Geo.haversine(p, dest) <= JourneyConfig.ARRIVAL_M) { arrived = true; return listOf(JourneyEvent.ARRIVED) }

        // deviation
        if (route.size >= 2 && Geo.distanceToRoute(p, route) > JourneyConfig.DEVIATION_M) {
            if (deviatedSince < 0) deviatedSince = tMs
            if (!deviatedLatched && tMs - deviatedSince > JourneyConfig.DEVIATION_SEC * 1000) { deviatedLatched = true; out += JourneyEvent.DEVIATED }
        } else { deviatedSince = -1; deviatedLatched = false }

        // stop
        val a = anchor
        if (a == null || Geo.haversine(a, p) > JourneyConfig.STOP_RADIUS_M) {
            anchor = p; anchorSince = tMs; stoppedLatched = false
        } else if (!stoppedLatched && tMs - anchorSince >= JourneyConfig.STOP_MIN_SEC * 1000 &&
            safePlaces.none { Geo.haversine(it, p) <= JourneyConfig.SAFE_PLACE_RADIUS_M }
        ) { stoppedLatched = true; out += JourneyEvent.STOPPED }

        out += onTick(tMs)
        if (battery != null && battery < JourneyConfig.LOW_BATTERY_PCT && !lowBatLatched) { lowBatLatched = true; out += JourneyEvent.LOW_BATTERY }
        return out
    }

    fun onTick(tMs: Long): List<JourneyEvent> {
        if (!arrived && !delayedLatched && tMs > etaMs + bufferMs) { delayedLatched = true; return listOf(JourneyEvent.DELAYED) }
        return emptyList()
    }
}
