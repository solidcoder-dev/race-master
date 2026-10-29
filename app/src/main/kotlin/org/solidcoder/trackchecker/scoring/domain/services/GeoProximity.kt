package org.solidcoder.trackchecker.scoring.domain.services

import kotlin.math.*
import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.tracks.domain.model.GpsSample

/** Distances in meters, using geodesic endpoints and a local projection for short GPS segments. */
internal object GeoProximity {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun between(point: Coordinates, sample: GpsSample): Double {
        val latitudeDelta = Math.toRadians(sample.latitude - point.latitude)
        val longitudeDelta = Math.toRadians(sample.longitude - point.longitude)
        val a = sin(latitudeDelta / 2).pow(2.0) +
            cos(Math.toRadians(point.latitude)) * cos(Math.toRadians(sample.latitude)) *
            sin(longitudeDelta / 2).pow(2.0)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun segmentDistance(point: Coordinates, start: GpsSample, end: GpsSample): Double {
        val latitudeScale = Math.PI * EARTH_RADIUS_METERS / 180.0
        val ax = Math.toRadians(start.longitude - point.longitude) * EARTH_RADIUS_METERS * cos(Math.toRadians(point.latitude))
        val ay = (start.latitude - point.latitude) * latitudeScale
        val bx = Math.toRadians(end.longitude - point.longitude) * EARTH_RADIUS_METERS * cos(Math.toRadians(point.latitude))
        val by = (end.latitude - point.latitude) * latitudeScale
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        val fraction = if (lengthSquared == 0.0) 0.0 else (-(ax * dx + ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
        return hypot(ax + fraction * dx, ay + fraction * dy)
    }
}
