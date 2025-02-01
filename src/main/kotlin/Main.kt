package org.picolobruno.racing

import domain.entities.Route
import domain.entities.RoutePoint
import domain.entities.Track
import domain.entities.TrackPoint
import domain.services.DistanceStrategy
import domain.services.HaversineDistanceStrategy
import domain.services.TrackValidator

fun main() {
    val route = Route(
        points = listOf(
            RoutePoint("Point A", 37.7749, -122.4194),
            RoutePoint("Point B", 37.7750, -122.4195),
            RoutePoint("Point C", 37.7751, -122.4196)
        ),
        start = RoutePoint("Start", 37.7748, -122.4193),
        end = RoutePoint("End", 37.7752, -122.4197)
    )

    val track = Track(
        listOf(
            TrackPoint(1706792231000, 37.7748, -122.4193), // Start (valid)
            TrackPoint(1706792232000, 37.7749, -122.4194), // Point A (valid)
            TrackPoint(1706792233000, 37.7750, -122.4195), // Point B (valid)
            TrackPoint(1706792234000, 37.7753, -122.4198), // Not matching any point
            TrackPoint(1706792235000, 37.7752, -122.4197)  // End (valid)
        )
    )

    val toleranceKm = 0.2
    val strategy: DistanceStrategy = HaversineDistanceStrategy()
    val validator = TrackValidator(strategy, toleranceKm)

    val result = validator.validate(track, route)

    // Print validation results
    println("Start point validation: ${if (result.isStartValid) "PASSED ✅" else "FAILED ❌"}")
    println("End point validation: ${if (result.isEndValid) "PASSED ✅" else "FAILED ❌"}")

    println("Route points reached:")
    result.reachedPoints.forEach { println("- ${it.name} ✅") }

    if (result.missedPoints.isNotEmpty()) {
        println("Route points missed:")
        result.missedPoints.forEach { println("- ${it.name} ❌") }
    }

}