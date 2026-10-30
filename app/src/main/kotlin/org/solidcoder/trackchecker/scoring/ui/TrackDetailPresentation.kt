package org.solidcoder.trackchecker.scoring.ui

import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus
import org.solidcoder.trackchecker.scoring.domain.model.TrackEvaluation

enum class TrackPointFilter { ALL, PASSED, MISSED, UNCERTAIN }

fun filterTrackPoints(
    route: Route,
    evaluation: TrackEvaluation,
    filter: TrackPointFilter
): List<RoutePoint> {
    val passages = evaluation.passageReport.passages.associateBy { it.pointId }
    return route.points.filter { point ->
        when (filter) {
            TrackPointFilter.ALL -> true
            TrackPointFilter.PASSED -> passages[point.id]?.status == PassageStatus.PASSED
            TrackPointFilter.MISSED -> passages[point.id]?.status == PassageStatus.MISSED
            TrackPointFilter.UNCERTAIN -> passages[point.id]?.status == PassageStatus.UNCERTAIN
        }
    }
}
