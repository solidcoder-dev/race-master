package org.solidcoder.trackchecker.scoring.ui

import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.scoring.application.usecases.EvaluatedTrackSummary

fun sortTrackSummaries(
    route: Route,
    tracks: List<EvaluatedTrackSummary>
): List<EvaluatedTrackSummary> = tracks.sortedWith(
    compareBy<EvaluatedTrackSummary> { it.evaluation.score == null }
        .thenByDescending { it.evaluation.score?.total ?: Int.MIN_VALUE }
)
