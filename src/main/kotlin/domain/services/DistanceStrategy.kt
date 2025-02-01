package domain.services

import domain.entities.Point

interface DistanceStrategy {
    fun arePointsClose(p1: Point, p2: Point, tolerance: Double): Boolean
}

