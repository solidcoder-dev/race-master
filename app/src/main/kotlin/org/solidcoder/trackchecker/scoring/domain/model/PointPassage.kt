package org.solidcoder.trackchecker.scoring.domain.model

import org.solidcoder.trackchecker.routes.domain.model.RoutePointId

enum class PassageStatus { PASSED, MISSED, UNCERTAIN }

data class PointPassage(
    val pointId: RoutePointId,
    val status: PassageStatus,
    val minimumDistanceMeters: Double,
    val detectionRadiusMeters: Double
)

data class TrackPassageReport(
    val passages: List<PointPassage>,
    val validStart: Boolean,
    val validFinish: Boolean
) {
    val needsReview: Boolean get() = !validStart || !validFinish || passages.any { it.status == PassageStatus.UNCERTAIN }
}
