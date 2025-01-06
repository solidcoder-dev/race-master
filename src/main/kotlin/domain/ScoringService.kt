package org.picolobruno.racing.domain

import kotlin.math.*

class ScoringService {

    fun calculateScore(
        track: Track,
        raceRoute: RaceRoute,
        tolerance: Tolerance
    ): Score {
        var totalPoints = Score(0)

        for (waypoint in raceRoute.waypoints) {
            val isHit = track.coordinates.any { coordinate ->
                distanceInKm(waypoint.coordinate, coordinate) <= tolerance.distanceInKm
            }
            if (isHit) {
                totalPoints = totalPoints.plus(waypoint.score)
            }
        }

        return totalPoints
    }

    /**
     * Calculate distance in kilometers between two coordinates using the Haversine formula.
     */
    private fun distanceInKm(c1: Coordinate, c2: Coordinate): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(c2.latitude - c1.latitude)
        val dLon = Math.toRadians(c2.longitude - c1.longitude)
        val lat1 = Math.toRadians(c1.latitude)
        val lat2 = Math.toRadians(c2.latitude)

        val a = sin(dLat / 2).pow(2) +
            sin(dLon / 2).pow(2) * cos(lat1) * cos(lat2)
        val c = 2 * asin(sqrt(a))

        return earthRadiusKm * c
    }
}
