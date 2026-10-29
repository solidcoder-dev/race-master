package org.solidcoder.trackchecker.scoring.domain

import org.junit.Assert.*
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus
import org.solidcoder.trackchecker.scoring.domain.services.TrackEvaluator
import org.solidcoder.trackchecker.tracks.domain.model.*

class TrackEvaluatorTest {
    private val route = Route.imported(RouteId("r"), "Route", listOf(
        point("start", 28.0, RoutePointType.START),
        point("wp", 28.001, RoutePointType.WAYPOINT),
        point("cp", 28.005, RoutePointType.CHECKPOINT),
        point("finish", 28.01, RoutePointType.FINISH)
    ), 0L)

    @Test fun `start and finish require actual first and last GPS positions`() {
        val track = track(28.0, 28.001, 28.01)
        val report = TrackEvaluator().evaluate(route, track)
        assertTrue(report.validStart)
        assertTrue(report.validFinish)
        assertEquals(PassageStatus.PASSED, report.passages[1].status)
        assertEquals(PassageStatus.MISSED, report.passages[2].status)
    }

    @Test fun `a waypoint between recorded samples is reached within its radius`() {
        val track = track(28.0, 28.002, 28.01)
        assertEquals(PassageStatus.PASSED, TrackEvaluator().evaluate(route, track).passages[1].status)
    }

    @Test fun `an impossible GPS jump cannot prove passage`() {
        val track = track(28.0, 28.01)
        val report = TrackEvaluator(maximumInterpolatedGapMeters = 100.0).evaluate(route, track)
        assertEquals(PassageStatus.UNCERTAIN, report.passages[1].status)
        assertTrue(report.needsReview)
    }

    @Test fun `wrong first position prevents a confirmed evaluation`() {
        val report = TrackEvaluator().evaluate(route, track(28.002, 28.01))
        assertFalse(report.validStart)
        assertTrue(report.needsReview)
    }

    private fun point(id: String, latitude: Double, type: RoutePointType) =
        RoutePoint(RoutePointId(id), id, Coordinates(latitude, -15.0), type)

    private fun track(vararg latitudes: Double) = Track(TrackId("t"), "t", listOf(
        TrackSegment(latitudes.map { GpsSample(it, -15.0) })
    ))
}
