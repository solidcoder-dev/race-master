package org.solidcoder.trackchecker.scoring.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.scoring.domain.model.*
import org.solidcoder.trackchecker.tracks.domain.model.TrackId

class TrackDetailPresentationTest {
    @Test
    fun `filters preserve original route point order`() {
        val route = route()
        val evaluation = evaluation(route, listOf(
            PointPassage(RoutePointId("wp"), PassageStatus.PASSED, 18.0, 50.0),
            PointPassage(RoutePointId("cp"), PassageStatus.MISSED, 127.0, 50.0),
            PointPassage(RoutePointId("uncertain"), PassageStatus.UNCERTAIN, 32.0, 50.0)
        ))

        assertEquals(listOf("start", "wp", "cp", "uncertain", "finish"), filterTrackPoints(route, evaluation, TrackPointFilter.ALL).map { it.id.value })
        assertEquals(listOf("wp"), filterTrackPoints(route, evaluation, TrackPointFilter.PASSED).map { it.id.value })
        assertEquals(listOf("cp"), filterTrackPoints(route, evaluation, TrackPointFilter.MISSED).map { it.id.value })
        assertEquals(listOf("uncertain"), filterTrackPoints(route, evaluation, TrackPointFilter.UNCERTAIN).map { it.id.value })
    }

    @Test
    fun `uncertain passage remains distinct from missed`() {
        val route = route()
        val evaluation = evaluation(route, listOf(
            PointPassage(RoutePointId("uncertain"), PassageStatus.UNCERTAIN, 32.0, 50.0)
        ))

        assertEquals(emptyList<RoutePoint>(), filterTrackPoints(route, evaluation, TrackPointFilter.MISSED))
        assertEquals(listOf("uncertain"), filterTrackPoints(route, evaluation, TrackPointFilter.UNCERTAIN).map { it.id.value })
    }

    private fun evaluation(route: Route, passages: List<PointPassage>) = TrackEvaluation(
        EvaluationId("evaluation"), route.id, TrackId("track"), route.evaluationFingerprint(), 1L,
        TrackPassageReport(passages, validStart = true, validFinish = true), null
    )

    private fun route() = Route.imported(RouteId("route"), "Route", listOf(
        point("start", RoutePointType.START), point("wp", RoutePointType.WAYPOINT),
        point("cp", RoutePointType.CHECKPOINT), point("uncertain", RoutePointType.WAYPOINT),
        point("finish", RoutePointType.FINISH)
    ), 1L)

    private fun point(id: String, type: RoutePointType) = RoutePoint(
        RoutePointId(id), id, Coordinates(0.0, 0.0), type
    )
}
