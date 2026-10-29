package org.solidcoder.trackchecker.scoring.domain.model

import java.util.UUID
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.tracks.domain.model.TrackId

@JvmInline
value class EvaluationId(val value: String) {
    init { require(value.isNotBlank()) }
    companion object { fun new(): EvaluationId = EvaluationId(UUID.randomUUID().toString()) }
}

enum class EvaluationStatus { PROVISIONAL, NEEDS_REVIEW }

class TrackEvaluation(
    val id: EvaluationId,
    val routeId: RouteId,
    val trackId: TrackId,
    val routeFingerprint: String,
    val evaluatedAtEpochMs: Long,
    val passageReport: TrackPassageReport,
    val score: Score?
) {
    init {
        require(routeFingerprint.isNotBlank())
        require(evaluatedAtEpochMs >= 0)
        require(passageReport.passages.map { it.pointId }.distinct().size == passageReport.passages.size)
    }

    val status: EvaluationStatus get() = if (passageReport.needsReview)
        EvaluationStatus.NEEDS_REVIEW else EvaluationStatus.PROVISIONAL

    fun isOutdatedFor(route: Route): Boolean =
        route.id != routeId || route.evaluationFingerprint() != routeFingerprint
}
