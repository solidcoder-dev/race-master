package domain.services

import domain.entities.Route
import domain.entities.Track
import domain.entities.ValidationResult

class TrackValidator(private val strategy: DistanceStrategy, private val tolerance: Double) {

    fun validate(track: Track, route: Route): ValidationResult {
        if (track.isEmpty()) return ValidationResult(false, false, emptyList(), route.points)

        val isStartValid = track.firstPoint()?.let { strategy.arePointsClose(it, route.start, tolerance) } ?: false
        val isEndValid = track.lastPoint()?.let { strategy.arePointsClose(it, route.end, tolerance) } ?: false

        val reachedPoints = route.points.filter { routePoint ->
            track.points.any { trackPoint -> strategy.arePointsClose(routePoint, trackPoint, tolerance) }
        }

        val missedPoints = route.points - reachedPoints.toSet()

        return ValidationResult(isStartValid, isEndValid, reachedPoints, missedPoints.toList())
    }
}
