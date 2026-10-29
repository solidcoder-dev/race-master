package org.solidcoder.trackchecker.scoring.domain.repositories

import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.scoring.domain.model.TrackEvaluation
import org.solidcoder.trackchecker.tracks.domain.model.TrackId

interface EvaluationRepository {
    suspend fun save(evaluation: TrackEvaluation)
    suspend fun find(routeId: RouteId, trackId: TrackId): TrackEvaluation?
    suspend fun listForRoute(routeId: RouteId): List<TrackEvaluation>
}
