package org.solidcoder.trackchecker.scoring.application.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RouteStatus
import org.solidcoder.trackchecker.routes.domain.repositories.RouteRepository
import org.solidcoder.trackchecker.routes.domain.services.RouteValidator
import org.solidcoder.trackchecker.scoring.domain.model.EvaluationId
import org.solidcoder.trackchecker.scoring.domain.model.TrackEvaluation
import org.solidcoder.trackchecker.scoring.domain.repositories.EvaluationRepository
import org.solidcoder.trackchecker.scoring.domain.services.ScoreCalculator
import org.solidcoder.trackchecker.scoring.domain.services.TrackEvaluator
import org.solidcoder.trackchecker.tracks.domain.model.TrackId
import org.solidcoder.trackchecker.tracks.domain.repositories.TrackRepository

class EvaluateTrackUseCase(
    private val routes: RouteRepository,
    private val tracks: TrackRepository,
    private val evaluations: EvaluationRepository,
    private val evaluator: TrackEvaluator = TrackEvaluator(),
    private val calculator: ScoreCalculator = ScoreCalculator(),
    private val validator: RouteValidator = RouteValidator(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> EvaluationId = EvaluationId::new,
    private val calculationDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(routeId: RouteId, trackId: TrackId): TrackEvaluation {
        val route = routes.findById(routeId) ?: error("Ruta no encontrada")
        require(route.status == RouteStatus.READY && validator.validate(route).canBeReady) {
            "Valida la ruta antes de evaluar tracks"
        }
        val track = tracks.findById(trackId) ?: error("Track no encontrado")
        require(track.routeId == routeId) { "El track no pertenece a esta ruta" }
        val fingerprint = route.evaluationFingerprint()
        val report = withContext(calculationDispatcher) { evaluator.evaluate(route, track) }
        val score = calculator.calculate(route, report)
        require(routes.findById(routeId)?.let { current ->
            current.status == RouteStatus.READY && current.evaluationFingerprint() == fingerprint
        } == true) { "La ruta ha cambiado durante la evaluación" }
        val evaluation = TrackEvaluation(
            id = evaluations.find(routeId, trackId)?.id ?: newId(),
            routeId = routeId,
            trackId = trackId,
            routeFingerprint = fingerprint,
            evaluatedAtEpochMs = clock(),
            passageReport = report,
            score = score
        )
        evaluations.save(evaluation)
        return evaluation
    }
}
