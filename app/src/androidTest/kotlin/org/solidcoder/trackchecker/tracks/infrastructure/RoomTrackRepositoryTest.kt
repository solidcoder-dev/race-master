package org.solidcoder.trackchecker.tracks.infrastructure

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoutesDatabase
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoomRouteRepository
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.tracks.domain.model.GpsSample
import org.solidcoder.trackchecker.tracks.domain.model.Track
import org.solidcoder.trackchecker.tracks.domain.model.TrackId
import org.solidcoder.trackchecker.tracks.domain.model.TrackSegment
import org.solidcoder.trackchecker.tracks.infrastructure.persistence.RoomTrackRepository

@RunWith(AndroidJUnit4::class)
class RoomTrackRepositoryTest {
    private lateinit var db: RoutesDatabase
    private lateinit var repository: RoomTrackRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoutesDatabase::class.java
        ).build()
        repository = RoomTrackRepository(db)
        runBlocking {
            RoomRouteRepository(db).save(Route.imported(RouteId("route"), "Route", listOf(
                RoutePoint(RoutePointId("start"), "Start", Coordinates(28.0, -15.0), RoutePointType.START),
                RoutePoint(RoutePointId("finish"), "Finish", Coordinates(28.01, -15.01), RoutePointType.FINISH)
            ), 0L))
        }
    }

    @After fun tearDown() = db.close()

    @Test fun tracksWithEqualNamesRemainIndependentAndOrdered() = runBlocking {
        val first = Track(TrackId("a"), RouteId("route"), "participant.kml", listOf(
            TrackSegment(listOf(GpsSample(28.0, -15.0), GpsSample(28.1, -15.1))),
            TrackSegment(listOf(GpsSample(28.2, -15.2)))
        ))
        val second = Track(TrackId("b"), RouteId("route"), "participant.kml", listOf(
            TrackSegment(listOf(GpsSample(29.0, -16.0)))
        ))

        repository.save(first)
        repository.save(second)
        assertEquals(setOf(first.id, second.id), repository.listForRoute(RouteId("route")).map { it.id }.toSet())
        assertEquals(listOf(2, 1), repository.findById(first.id)!!.segments.map { it.samples.size })
        assertEquals(28.1, repository.findById(first.id)!!.segments.first().samples.last().latitude, 0.0)
        assertNotNull(repository.findById(second.id))

        repository.delete(first.id)
        assertNull(repository.findById(first.id))
        assertNotNull(repository.findById(second.id))

        RoomRouteRepository(db).delete(RouteId("route"))
        assertNull(repository.findById(second.id))
    }
}
