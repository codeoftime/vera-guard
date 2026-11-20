package com.vera.guard.journey

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double)

object Geo {
    private const val R = 6371000.0
    private fun rad(d: Double) = Math.toRadians(d)

    fun haversine(a: LatLng, b: LatLng): Double {
        val dLat = rad(b.lat - a.lat); val dLng = rad(b.lng - a.lng)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(rad(a.lat)) * cos(rad(b.lat)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * R * asin(sqrt(h))
    }

    /** Metres from p to the polyline (equirectangular projection; fine for city-scale segments). */
    fun distanceToRoute(p: LatLng, route: List<LatLng>): Double {
        if (route.isEmpty()) return Double.MAX_VALUE
        if (route.size == 1) return haversine(p, route[0])
        val k = cos(rad(p.lat))
        fun x(q: LatLng) = q.lng * k * 111320.0
        fun y(q: LatLng) = q.lat * 110540.0
        val px = x(p); val py = y(p)
        var best = Double.MAX_VALUE
        for (i in 0 until route.size - 1) {
            val ax = x(route[i]); val ay = y(route[i]); val bx = x(route[i + 1]); val by = y(route[i + 1])
            val dx = bx - ax; val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
            best = minOf(best, hypot(px - (ax + t * dx), py - (ay + t * dy)))
        }
        return best
    }
}
