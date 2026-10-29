package org.solidcoder.trackchecker.scoring.application.usecases

import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.scoring.domain.model.TrackEvaluation
import org.solidcoder.trackchecker.scoring.domain.repositories.EvaluationRepository
import org.solidcoder.trackchecker.tracks.application.ports.TrackImportSource
import org.solidcoder.trackchecker.tracks.application.usecases.ImportTrackUseCase
import org.solidcoder.trackchecker.tracks.domain.model.Track
import org.solidcoder.trackchecker.tracks.domain.model.TrackId
import org.solidcoder.trackchecker.tracks.domain.repositories.TrackRepository

data class EvaluatedTrack(val track: Track, val evaluation: TrackEvaluation)

/** Coordinates imported data and evaluations without giving UI direct repository access. */
class TrackCheckActions(
    private val importTrack: ImportTrackUseCase,
    private val evaluate: EvaluateTrackUseCase,
    private val tracks: TrackRepository,
    private val evaluations: EvaluationRepository
) {
    suspend fun importAndEvaluate(routeId: RouteId, source: TrackImportSource): EvaluatedTrack {
        val track = importTrack(routeId, source)
        return try {
            EvaluatedTrack(track, evaluate(routeId, track.id))
        } catch (error: Exception) {
            // A failed import must not leave a track without its expected evaluation.
            tracks.delete(track.id)
            throw error
        }
    }

    suspend fun list(routeId: RouteId): List<EvaluatedTrack> =
        evaluations.listForRoute(routeId).mapNotNull { evaluation ->
            tracks.findById(evaluation.trackId)?.let { EvaluatedTrack(it, evaluation) }
        }

    suspend fun recalculate(routeId: RouteId, trackId: TrackId): TrackEvaluation = evaluate(routeId, trackId)

    suspend fun delete(routeId: RouteId, trackId: TrackId) {
        require(evaluations.find(routeId, trackId) != null) { "Track no asociado a esta ruta" }
        tracks.delete(trackId)
    }
}
