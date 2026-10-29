package org.solidcoder.trackchecker.scoring.application

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.domain.repositories.RouteRepository
import org.solidcoder.trackchecker.scoring.application.usecases.EvaluateTrackUseCase
import org.solidcoder.trackchecker.scoring.domain.model.TrackEvaluation
import org.solidcoder.trackchecker.scoring.domain.repositories.EvaluationRepository
import org.solidcoder.trackchecker.tracks.domain.model.*
import org.solidcoder.trackchecker.tracks.domain.repositories.TrackRepository

class EvaluateTrackUseCaseTest {
    private val route = Route.imported(RouteId("r"), "Stage", listOf(
        point("start", 28.0, RoutePointType.START),
        point("wp", 28.001, RoutePointType.WAYPOINT),
        point("cp", 28.005, RoutePointType.CHECKPOINT),
        point("finish", 28.01, RoutePointType.FINISH)
    ), 0L, scoringPattern = ScoringPattern.MIXED).markReady()

    @Test fun `each track gets its own persisted score`() = runBlocking {
        val tracks = MemoryTracks()
        val evaluations = MemoryEvaluations()
        val first = track("first", listOf(
            GpsSample(28.0, -15.0), GpsSample(28.001, -15.0),
            GpsSample(28.001, -15.01), GpsSample(28.01, -15.01), GpsSample(28.01, -15.0)
        ))
        val second = track("second", listOf(
            GpsSample(28.0, -15.0), GpsSample(28.0, -15.01),
            GpsSample(28.005, -15.01), GpsSample(28.005, -15.0), GpsSample(28.01, -15.0)
        ))
        tracks.save(first)
        tracks.save(second)
        val action = EvaluateTrackUseCase(MemoryRoutes(route), tracks, evaluations)

        assertEquals(-10, action(route.id, first.id).score?.total)
        assertEquals(0, action(route.id, second.id).score?.total)
        assertEquals(2, evaluations.listForRoute(route.id).size)
    }

    @Test fun `changing route scoring marks previous evaluation outdated`() = runBlocking {
        val tracks = MemoryTracks()
        val evaluations = MemoryEvaluations()
        val sample = track("a", listOf(GpsSample(28.0, -15.0), GpsSample(28.001, -15.0), GpsSample(28.01, -15.0)))
        tracks.save(sample)
        val result = EvaluateTrackUseCase(MemoryRoutes(route), tracks, evaluations)(route.id, sample.id)
        assertFalse(result.isOutdatedFor(route))
        assertTrue(result.isOutdatedFor(route.changeDefaultDetectionRadius(DetectionRadius(75.0))))
        assertFalse(result.isOutdatedFor(route.rename("New display name")))
    }

    @Test fun `a track cannot be evaluated for a different route`() = runBlocking {
        val tracks = MemoryTracks()
        val evaluations = MemoryEvaluations()
        val foreign = Track(
            TrackId("foreign"), RouteId("another-route"), "GPS",
            listOf(TrackSegment(listOf(GpsSample(28.0, -15.0))))
        )
        tracks.save(foreign)
        val action = EvaluateTrackUseCase(MemoryRoutes(route), tracks, evaluations)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { action(route.id, foreign.id) }
        }
        assertTrue(evaluations.listForRoute(route.id).isEmpty())
    }

    private fun point(id: String, lat: Double, type: RoutePointType) =
        RoutePoint(RoutePointId(id), id, Coordinates(lat, -15.0), type)

    private fun track(id: String, samples: List<GpsSample>) = Track(TrackId(id), route.id, id,
        listOf(TrackSegment(samples)))

    private class MemoryRoutes(private val route: Route) : RouteRepository {
        override suspend fun findById(id: RouteId): Route? = route.takeIf { it.id == id }
        override suspend fun save(route: Route) = Unit
        override suspend fun delete(id: RouteId) = Unit
    }

    private class MemoryTracks : TrackRepository {
        private val tracks = mutableMapOf<TrackId, Track>()
        override suspend fun findById(id: TrackId): Track? = tracks[id]
        override suspend fun listForRoute(routeId: RouteId) = tracks.values
            .filter { it.routeId == routeId }
            .map { TrackSummary(it.id, it.routeId, it.name, it.importedAtEpochMs) }
        override suspend fun save(track: Track) { tracks[track.id] = track }
        override suspend fun delete(id: TrackId) { tracks.remove(id) }
    }

    private class MemoryEvaluations : EvaluationRepository {
        private val items = mutableMapOf<Pair<RouteId, TrackId>, TrackEvaluation>()
        override suspend fun save(evaluation: TrackEvaluation) {
            items[evaluation.routeId to evaluation.trackId] = evaluation
        }
        override suspend fun find(routeId: RouteId, trackId: TrackId): TrackEvaluation? = items[routeId to trackId]
        override suspend fun listForRoute(routeId: RouteId): List<TrackEvaluation> =
            items.values.filter { it.routeId == routeId }
    }
}
