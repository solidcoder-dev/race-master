package domain.services

import domain.entities.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class HaversineDistanceStrategy : DistanceStrategy {
    private val EARTH_RADIUS_KM = 6371.0

    override fun arePointsClose(p1: Point, p2: Point, tolerance: Double): Boolean {
        val distance = haversineDistanceKm(p1, p2)
        return distance <= tolerance
    }

    private fun haversineDistanceKm(p1: Point, p2: Point): Double {
        val latDiff = Math.toRadians(p2.latitude - p1.latitude)
        val lonDiff = Math.toRadians(p2.longitude - p1.longitude)

        val a = sin(latDiff / 2).pow(2) +
            cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) * sin(lonDiff / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }
}
