package domain.entities

import domain.services.DistanceStrategy


class Route(val points: List<RoutePoint>, val start: RoutePoint, val end: RoutePoint) {
    fun containsPoint(trackPoint: TrackPoint, strategy: DistanceStrategy, tolerance: Double): Boolean {
        return points.any { strategy.arePointsClose(it, trackPoint, tolerance) }
    }
}
