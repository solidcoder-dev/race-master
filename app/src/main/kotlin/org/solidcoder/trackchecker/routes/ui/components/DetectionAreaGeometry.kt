package org.solidcoder.trackchecker.routes.ui.components

import kotlin.math.*
import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.routes.domain.model.DetectionRadius

/** Approximates a geodesic disk in metres, independently of map zoom or latitude. */
internal object DetectionAreaGeometry {
    private const val EARTH_RADIUS_METERS = 6_371_008.8

    fun ring(center: Coordinates, radius: DetectionRadius, segments: Int = 48): List<Coordinates> {
        require(segments >= 8)
        val latitude = Math.toRadians(center.latitude)
        val longitude = Math.toRadians(center.longitude)
        val angularDistance = radius.meters / EARTH_RADIUS_METERS
        val ring = (0 until segments).map { step ->
            val bearing = 2.0 * PI * step / segments
            val north = asin(
                sin(latitude) * cos(angularDistance) +
                    cos(latitude) * sin(angularDistance) * cos(bearing)
            )
            val east = longitude + atan2(
                sin(bearing) * sin(angularDistance) * cos(latitude),
                cos(angularDistance) - sin(latitude) * sin(north)
            )
            Coordinates(Math.toDegrees(north), normalizeLongitude(Math.toDegrees(east)))
        }
        return ring + ring.first()
    }

    private fun normalizeLongitude(degrees: Double): Double = ((degrees + 540) % 360) - 180
}
