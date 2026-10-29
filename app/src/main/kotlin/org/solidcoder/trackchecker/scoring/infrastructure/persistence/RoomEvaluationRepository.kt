package org.solidcoder.trackchecker.scoring.infrastructure.persistence

import androidx.room.withTransaction
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoutesDatabase
import org.solidcoder.trackchecker.scoring.domain.model.*
import org.solidcoder.trackchecker.scoring.domain.repositories.EvaluationRepository
import org.solidcoder.trackchecker.tracks.domain.model.TrackId

class RoomEvaluationRepository(private val database: RoutesDatabase) : EvaluationRepository {
    private val dao = database.evaluations()

    override suspend fun find(routeId: RouteId, trackId: TrackId): TrackEvaluation? =
        dao.find(routeId.value, trackId.value)?.toDomain()

    override suspend fun listForRoute(routeId: RouteId): List<TrackEvaluation> =
        dao.listForRoute(routeId.value).map { it.toDomain() }

    override suspend fun save(evaluation: TrackEvaluation) {
        database.withTransaction {
            dao.upsert(EvaluationRecord(
                id = evaluation.id.value,
                routeId = evaluation.routeId.value,
                trackId = evaluation.trackId.value,
                routeFingerprint = evaluation.routeFingerprint,
                evaluatedAtEpochMs = evaluation.evaluatedAtEpochMs,
                validStart = evaluation.passageReport.validStart,
                validFinish = evaluation.passageReport.validFinish,
                scoreTotal = evaluation.score?.total
            ))
            dao.deletePoints(evaluation.id.value)
            val contributions = evaluation.score?.contributions?.associate { it.pointId to it.points }.orEmpty()
            dao.insertPoints(evaluation.passageReport.passages.map { passage ->
                EvaluatedPointRecord(
                    evaluationId = evaluation.id.value,
                    pointId = passage.pointId.value,
                    status = passage.status.name,
                    minimumDistanceMeters = passage.minimumDistanceMeters,
                    detectionRadiusMeters = passage.detectionRadiusMeters,
                    scoreContribution = contributions[passage.pointId]
                )
            })
        }
    }

    private fun EvaluationWithPoints.toDomain(): TrackEvaluation {
        val contributions = points.mapNotNull { record ->
            record.scoreContribution?.let { PointContribution(RoutePointId(record.pointId), it) }
        }
        return TrackEvaluation(
            id = EvaluationId(evaluation.id),
            routeId = RouteId(evaluation.routeId),
            trackId = TrackId(evaluation.trackId),
            routeFingerprint = evaluation.routeFingerprint,
            evaluatedAtEpochMs = evaluation.evaluatedAtEpochMs,
            passageReport = TrackPassageReport(
                passages = points.map { record ->
                    PointPassage(RoutePointId(record.pointId), PassageStatus.valueOf(record.status),
                        record.minimumDistanceMeters, record.detectionRadiusMeters)
                },
                validStart = evaluation.validStart,
                validFinish = evaluation.validFinish
            ),
            score = evaluation.scoreTotal?.let { Score(it, contributions) }
        )
    }
}
