package org.solidcoder.trackchecker.scoring.domain.services

import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus
import org.solidcoder.trackchecker.scoring.domain.model.PointPassage
import org.solidcoder.trackchecker.scoring.domain.model.TrackPassageReport
import org.solidcoder.trackchecker.tracks.domain.model.Track
import org.solidcoder.trackchecker.tracks.domain.model.GpsSample

/** No Android, IO, persistence or scoring logic in geographical evaluation. */
class TrackEvaluator(
    private val maximumInterpolatedGapMeters: Double = 1000.0
) {
    init { require(maximumInterpolatedGapMeters.isFinite() && maximumInterpolatedGapMeters > 0.0) }

    fun evaluate(route: Route, track: Track): TrackPassageReport {
        val startId = route.points.single { it.type == RoutePointType.START }.id
        val finishId = route.points.single { it.type == RoutePointType.FINISH }.id
        val passages = route.points.map { point ->
            val radius = route.detectionRadiusFor(point.id).meters
            val result = when (point.type) {
                RoutePointType.START -> endpointPassage(point, track.firstSample, radius)
                RoutePointType.FINISH -> endpointPassage(point, track.lastSample, radius)
                else -> pathPassage(point, track, radius)
            }
            PointPassage(point.id, result.status, result.distanceMeters, radius)
        }
        return TrackPassageReport(
            passages,
            passages.single { it.pointId == startId }.status == PassageStatus.PASSED,
            passages.single { it.pointId == finishId }.status == PassageStatus.PASSED
        )
    }

    private fun endpointPassage(point: RoutePoint, sample: GpsSample, radius: Double): ProximityResult {
        val distance = GeoProximity.between(point.coordinates, sample)
        return ProximityResult(if (distance <= radius) PassageStatus.PASSED else PassageStatus.MISSED, distance)
    }

    private fun pathPassage(point: RoutePoint, track: Track, radius: Double): ProximityResult {
        var minimum = Double.POSITIVE_INFINITY
        var uncertain = false
        for (segment in track.segments) {
            for (sample in segment.samples) {
                minimum = minOf(minimum, GeoProximity.between(point.coordinates, sample))
            }
            for (index in 0 until segment.samples.lastIndex) {
                val start = segment.samples[index]
                val end = segment.samples[index + 1]
                val length = GeoProximity.between(Coordinates(start.latitude, start.longitude), end)
                val nearest = GeoProximity.segmentDistance(point.coordinates, start, end)
                if (length <= maximumInterpolatedGapMeters) {
                    minimum = minOf(minimum, nearest)
                } else if (nearest <= radius) {
                    uncertain = true
                }
            }
        }
        val status = when {
            minimum <= radius -> PassageStatus.PASSED
            uncertain -> PassageStatus.UNCERTAIN
            else -> PassageStatus.MISSED
        }
        return ProximityResult(status, minimum)
    }

    private data class ProximityResult(val status: PassageStatus, val distanceMeters: Double)
}
