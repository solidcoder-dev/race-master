package org.solidcoder.trackchecker.scoring.infrastructure

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoomRouteRepository
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoutesDatabase
import org.solidcoder.trackchecker.scoring.domain.model.*
import org.solidcoder.trackchecker.scoring.infrastructure.persistence.RoomEvaluationRepository
import org.solidcoder.trackchecker.tracks.domain.model.*
import org.solidcoder.trackchecker.tracks.infrastructure.persistence.RoomTrackRepository

@RunWith(AndroidJUnit4::class)
class RoomEvaluationRepositoryTest {
    private lateinit var database: RoutesDatabase

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoutesDatabase::class.java
        ).build()
    }

    @After fun tearDown() = database.close()

    @Test fun multipleTrackScoresRoundTripIndependently() = runBlocking {
        val route = Route.imported(RouteId("r"), "Route", listOf(
            RoutePoint(RoutePointId("start"), "Start", Coordinates(28.0, -15.0), RoutePointType.START),
            RoutePoint(RoutePointId("wp"), "WP", Coordinates(28.1, -15.1), RoutePointType.WAYPOINT),
            RoutePoint(RoutePointId("cp"), "CP", Coordinates(28.2, -15.2), RoutePointType.CHECKPOINT),
            RoutePoint(RoutePointId("finish"), "Finish", Coordinates(28.3, -15.3), RoutePointType.FINISH)
        ), 0L).markReady()
        RoomRouteRepository(database).save(route)
        val tracks = RoomTrackRepository(database)
        val repository = RoomEvaluationRepository(database)
        for (index in 1..2) {
            val id = TrackId("track$index")
            tracks.save(Track(id, "Track $index", listOf(TrackSegment(listOf(GpsSample(28.0, -15.0))))))
            val points = route.points.map { point ->
                PointPassage(point.id, PassageStatus.PASSED, 0.0, 50.0)
            }
            repository.save(TrackEvaluation(
                EvaluationId("evaluation$index"), route.id, id, route.evaluationFingerprint(),
                100L + index, TrackPassageReport(points, true, true),
                Score(index * 10, listOf(PointContribution(RoutePointId("wp"), index * 10)))
            ))
        }
        val results = repository.listForRoute(route.id)
        assertEquals(2, results.size)
        assertEquals(setOf(10, 20), results.mapNotNull { it.score?.total }.toSet())
        assertEquals(4, results.first().passageReport.passages.size)
    }
}
