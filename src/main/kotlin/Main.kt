package org.picolobruno.racing

import domain.services.DistanceStrategy
import domain.services.HaversineDistanceStrategy
import domain.services.TrackValidator
import java.io.File
import java.io.FileInputStream
import org.picolobruno.racing.kml.application.KmlParser

fun main() {
    val kmlParser = KmlParser()

    // Parsing a Track
    val trackInputStream = File("/Users/brunopicolo/dev/race-master/src/main/resources/ET1 TRACK NOMAD 2024.kml")
    val track = kmlParser.parseTrack(FileInputStream(trackInputStream))

    // Parsing a Route
    val routeInputStream = File("/Users/brunopicolo/dev/race-master/src/main/resources/ET 1 WP NOMAD 2024.kml")
    val route = kmlParser.parseRoute(FileInputStream(routeInputStream))

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