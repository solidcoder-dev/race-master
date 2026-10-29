package org.solidcoder.trackchecker.tracks.application

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.domain.repositories.RouteRepository
import org.solidcoder.trackchecker.tracks.application.ports.TrackImportSource
import org.solidcoder.trackchecker.tracks.application.ports.TrackSourceReader
import org.solidcoder.trackchecker.tracks.application.usecases.ImportTrackUseCase
import org.solidcoder.trackchecker.tracks.domain.model.*
import org.solidcoder.trackchecker.tracks.domain.repositories.TrackRepository

class ImportTrackUseCaseTest {
    private val routeId = RouteId("route")
    private val validRoute = Route.imported(routeId, "ET1", listOf(
        RoutePoint(RoutePointId("s"), "Salida", Coordinates(28.0, -15.0), RoutePointType.START),
        RoutePoint(RoutePointId("w"), "WP01", Coordinates(28.1, -15.1), RoutePointType.WAYPOINT),
        RoutePoint(RoutePointId("c"), "CP01", Coordinates(28.2, -15.2), RoutePointType.CHECKPOINT),
        RoutePoint(RoutePointId("f"), "Meta", Coordinates(28.3, -15.3), RoutePointType.FINISH)
    ), 0L)

    @Test fun `draft routes reject imports before parsing or persistence`() = runBlocking {
        val tracks = MemoryTracks()
        var parsed = false
        val action = ImportTrackUseCase(MemoryRoutes(validRoute), tracks, TrackSourceReader {
            parsed = true
            listOf(TrackSegment(listOf(GpsSample(28.0, -15.0))))
        })
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { action(routeId, TrackImportSource("track.gpx", byteArrayOf())) }
        }
        assertEquals(false, parsed)
        assertEquals(0, tracks.saved.size)
    }

    @Test fun `each import into a ready route creates an independent track`() = runBlocking {
        val tracks = MemoryTracks()
        var nextId = 0
        val action = ImportTrackUseCase(MemoryRoutes(validRoute.markReady()), tracks,
            TrackSourceReader { listOf(TrackSegment(listOf(GpsSample(28.0, -15.0)))) },
            newId = { TrackId("track-${++nextId}") })
        val first = action(routeId, TrackImportSource("participant.gpx", byteArrayOf()))
        val second = action(routeId, TrackImportSource("participant.gpx", byteArrayOf()))
        assertEquals(2, tracks.saved.size)
        assertEquals("participant", first.name)
        assertEquals("participant", second.name)
        assertEquals(false, first.id == second.id)
    }

    private class MemoryRoutes(var route: Route) : RouteRepository {
        override suspend fun findById(id: RouteId): Route? = route.takeIf { it.id == id }
        override suspend fun save(route: Route) { this.route = route }
        override suspend fun delete(id: RouteId) = Unit
    }

    private class MemoryTracks : TrackRepository {
        val saved = mutableMapOf<TrackId, Track>()
        override suspend fun findById(id: TrackId): Track? = saved[id]
        override suspend fun listForRoute(routeId: RouteId) = saved.values
            .filter { it.routeId == routeId }
            .map { TrackSummary(it.id, it.routeId, it.name, it.importedAtEpochMs) }
        override suspend fun save(track: Track) { saved[track.id] = track }
        override suspend fun delete(id: TrackId) { saved.remove(id) }
    }
}
