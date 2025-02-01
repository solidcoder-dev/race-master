package domain.services

import domain.entities.Point
import kotlin.math.pow
import kotlin.math.sqrt

class EuclideanDistanceStrategy : DistanceStrategy {
    override fun arePointsClose(p1: Point, p2: Point, tolerance: Double): Boolean {
        val distance = sqrt((p1.latitude - p2.latitude).pow(2) + (p1.longitude - p2.longitude).pow(2))
        return distance <= tolerance
    }
}
