package org.solidcoder.trackchecker.scoring.application

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.domain.repositories.RouteRepository
import org.solidcoder.trackchecker.scoring.application.usecases.EvaluateTrackUseCase
import org.solidcoder.trackchecker.scoring.application.usecases.TrackCheckActions
import org.solidcoder.trackchecker.scoring.domain.model.TrackEvaluation
import org.solidcoder.trackchecker.scoring.domain.model.EvaluationId
import org.solidcoder.trackchecker.scoring.domain.model.TrackPassageReport
import org.solidcoder.trackchecker.scoring.domain.repositories.EvaluationRepository
import org.solidcoder.trackchecker.tracks.application.ports.TrackImportSource
import org.solidcoder.trackchecker.tracks.application.ports.TrackSourceReader
import org.solidcoder.trackchecker.tracks.application.usecases.ImportTrackUseCase
import org.solidcoder.trackchecker.tracks.domain.model.*
import org.solidcoder.trackchecker.tracks.domain.repositories.TrackRepository

class TrackCheckActionsTest {
    @Test fun `listing scores never loads full GPS recordings`() = runBlocking {
        val routeId = RouteId("route")
        val trackId = TrackId("track")
        val summary = TrackSummary(trackId, routeId, "Participant", 100L)
        val tracks = object : TrackRepository {
            override suspend fun findById(id: TrackId): Track? =
                error("Listing must not load track samples")
            override suspend fun listForRoute(routeId: RouteId) = listOf(summary)
            override suspend fun save(track: Track) = Unit
            override suspend fun delete(id: TrackId) = Unit
        }
        val evaluation = TrackEvaluation(
            EvaluationId("evaluation"),
            routeId, trackId, "fingerprint", 100L,
            TrackPassageReport(emptyList(), true, true),
            score = null
        )
        val evaluations = object : EvaluationRepository {
            override suspend fun save(evaluation: TrackEvaluation) = Unit
            override suspend fun find(routeId: RouteId, trackId: TrackId) = evaluation
            override suspend fun listForRoute(routeId: RouteId) = listOf(evaluation)
        }
        val routes = object : RouteRepository {
            override suspend fun findById(id: RouteId): Route? = null
            override suspend fun save(route: Route) = Unit
            override suspend fun delete(id: RouteId) = Unit
        }
        val actions = TrackCheckActions(
            ImportTrackUseCase(routes, tracks, TrackSourceReader { emptyList() }),
            EvaluateTrackUseCase(routes, tracks, evaluations),
            tracks, evaluations
        )

        val results = actions.list(routeId)
        assertEquals(listOf(summary), results.map { it.track })
        assertEquals(listOf(evaluation), results.map { it.evaluation })
    }

    @Test fun `cancelled evaluation rolls back its imported track`() = runBlocking {
        val route = Route.imported(RouteId("route"), "Stage", listOf(
            RoutePoint(RoutePointId("start"), "Start", Coordinates(28.0, -15.0), RoutePointType.START),
            RoutePoint(RoutePointId("wp"), "WP", Coordinates(28.001, -15.0), RoutePointType.WAYPOINT),
            RoutePoint(RoutePointId("cp"), "CP", Coordinates(28.005, -15.0), RoutePointType.CHECKPOINT),
            RoutePoint(RoutePointId("finish"), "Finish", Coordinates(28.01, -15.0), RoutePointType.FINISH)
        ), 0L).markReady()
        val routes = object : RouteRepository {
            override suspend fun findById(id: RouteId) = route.takeIf { it.id == id }
            override suspend fun save(route: Route) = Unit
            override suspend fun delete(id: RouteId) = Unit
        }
        val tracks = object : TrackRepository {
            val stored = mutableMapOf<TrackId, Track>()
            override suspend fun findById(id: TrackId) = stored[id]
            override suspend fun listForRoute(routeId: RouteId) = stored.values
                .filter { it.routeId == routeId }
                .map { TrackSummary(it.id, it.routeId, it.name, it.importedAtEpochMs) }
            override suspend fun save(track: Track) { stored[track.id] = track }
            override suspend fun delete(id: TrackId) {
                yield() // Suspended persistence must still complete after cancellation.
                stored.remove(id)
            }
        }
        val evaluations = object : EvaluationRepository {
            override suspend fun save(evaluation: TrackEvaluation) {
                currentCoroutineContext().cancel("Persistence cancelled")
                throw CancellationException("Persistence cancelled")
            }
            override suspend fun find(routeId: RouteId, trackId: TrackId): TrackEvaluation? = null
            override suspend fun listForRoute(routeId: RouteId) = emptyList<TrackEvaluation>()
        }
        val actions = TrackCheckActions(
            ImportTrackUseCase(routes, tracks, TrackSourceReader {
                listOf(TrackSegment(listOf(GpsSample(28.0, -15.0), GpsSample(28.01, -15.0))))
            }),
            EvaluateTrackUseCase(routes, tracks, evaluations),
            tracks,
            evaluations
        )

        val job = launch {
            try {
                actions.importAndEvaluate(route.id, TrackImportSource("participant.gpx", byteArrayOf()))
            } catch (_: CancellationException) {
                // The importer's caller was cancelled.
            }
        }
        job.join()
        assertTrue(tracks.stored.isEmpty())
    }
}
