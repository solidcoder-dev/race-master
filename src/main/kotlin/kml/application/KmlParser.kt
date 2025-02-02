package org.picolobruno.racing.kml.application

import domain.entities.Route
import domain.entities.RoutePoint
import domain.entities.Track
import domain.entities.TrackPoint
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

class KmlParser {

    fun parseTrack(kmlTrack: InputStream): Track {
        val document = parseXml(kmlTrack)

        val timestamps = extractTrackTimestamps(document)
        val coordinates = extractTrackCoordinates(document)

        if (timestamps.size != coordinates.size) {
            println("Warning: Timestamps (${timestamps.size}) and Coordinates (${coordinates.size}) count mismatch.")
        }

        val trackPoints = mutableListOf<TrackPoint>()
        val minSize = minOf(timestamps.size, coordinates.size) // Ensures no index out of bounds

        for (i in 0 until minSize) {
            trackPoints.add(TrackPoint(timestamps[i], coordinates[i].first, coordinates[i].second))
        }

        return Track(trackPoints)
    }

    private fun extractTrackTimestamps(document: Document): List<Long> {
        val timestamps = mutableListOf<Long>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val trainInfoNodes = document.getElementsByTagName("mwm:trainInfo")

        for (i in 0 until trainInfoNodes.length) {
            val trainInfoElement = trainInfoNodes.item(i) as? Element ?: continue
            val pointNodes = trainInfoElement.getElementsByTagName("mwm:point")

            for (j in 0 until pointNodes.length) {
                val pointElement = pointNodes.item(j) as? Element ?: continue
                val timestampNode = pointElement.getElementsByTagName("mwm:timestamp").item(0)

                timestampNode?.textContent?.trim()?.let { timestampStr ->
                    try {
                        val date = dateFormat.parse(timestampStr)
                        timestamps.add(date.time)
                    } catch (e: Exception) {
                        println("Skipping invalid timestamp: $timestampStr")
                    }
                }
            }
        }

        return timestamps
    }

    private fun extractTrackCoordinates(document: Document): List<Pair<Double, Double>> {
        val coordinateNodes = document.getElementsByTagName("coordinates")
        val coordinates = mutableListOf<Pair<Double, Double>>()

        for (i in 0 until coordinateNodes.length) {
            val coordinatesText = coordinateNodes.item(i).textContent.trim()
            val coordLines = coordinatesText.split(" ").filter { it.isNotBlank() }

            for (coord in coordLines) {
                val parts = coord.split(",")
                if (parts.size >= 2) {
                    val longitude = parts[0].toDoubleOrNull()
                    val latitude = parts[1].toDoubleOrNull()
                    if (longitude != null && latitude != null) {
                        coordinates.add(Pair(latitude, longitude))
                    }
                }
            }
        }

        return coordinates
    }

    private fun parseXml(inputStream: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(inputStream).apply { documentElement.normalize() }
    }

    fun parseRoute(kmlRoute: InputStream): Route {
        val document = parseXml(kmlRoute)

        val routePoints = extractRoutePoints(document)
        if (routePoints.isEmpty()) {
            throw IllegalArgumentException("Invalid KML: Route must contain at least one point.")
        }

        return Route(
            points = routePoints,
            start = routePoints.first(),
            end = routePoints.last()
        )
    }

    private fun extractRoutePoints(document: Document): List<RoutePoint> {
        val placemarks = document.getElementsByTagName("Placemark")
        val points = mutableListOf<RoutePoint>()

        for (i in 0 until placemarks.length) {
            val placemark = placemarks.item(i) as? Element ?: continue
            val name = placemark.getElementsByTagName("name").item(0)?.textContent ?: "Unknown"

            val coordinatesText = placemark.getElementsByTagName("coordinates").item(0)?.textContent?.trim()
            val coordinates = coordinatesText?.split(",") ?: continue
            if (coordinates.size < 2) continue

            val longitude = coordinates[0].toDoubleOrNull() ?: continue
            val latitude = coordinates[1].toDoubleOrNull() ?: continue

            points.add(RoutePoint(name, latitude, longitude))
        }
        return points
    }

}
