package org.solidcoder.trackchecker.scoring.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.scoring.application.usecases.EvaluatedTrackSummary
import org.solidcoder.trackchecker.scoring.domain.model.*
import org.solidcoder.trackchecker.tracks.domain.model.*

class TrackClassificationTest {
    @Test
    fun `sorts scored tracks descending and leaves equal scores stable`() {
        val route = testRoute()
        val first = summary(route, "first", 480)
        val equal = summary(route, "equal", 480)
        val highest = summary(route, "highest", 510)

        assertEquals(listOf("highest", "first", "equal"), sortTrackSummaries(route, listOf(first, equal, highest)).map { it.track.name })
    }

    @Test
    fun `puts tracks without a score after scored tracks`() {
        val route = testRoute()
        val missing = summary(route, "missing", null)
        val scored = summary(route, "scored", 0)

        assertEquals(listOf("scored", "missing"), sortTrackSummaries(route, listOf(missing, scored)).map { it.track.name })
    }

    private fun summary(route: Route, name: String, points: Int?): EvaluatedTrackSummary =
        EvaluatedTrackSummary(
            TrackSummary(TrackId(name), route.id, name, 1L),
            TrackEvaluation(
                EvaluationId(name), route.id, TrackId(name), route.evaluationFingerprint(), 1L,
                TrackPassageReport(emptyList(), validStart = true, validFinish = true),
                points?.let { Score(it, listOf(PointContribution(org.solidcoder.trackchecker.routes.domain.model.RoutePointId("start"), it))) }
            )
        )

    private fun testRoute(): Route = Route.imported(
        org.solidcoder.trackchecker.routes.domain.model.RouteId("route"),
        "Route",
        listOf(
            org.solidcoder.trackchecker.routes.domain.model.RoutePoint(
                org.solidcoder.trackchecker.routes.domain.model.RoutePointId("start"),
                "Start",
                org.solidcoder.trackchecker.routes.domain.model.Coordinates(0.0, 0.0),
                org.solidcoder.trackchecker.routes.domain.model.RoutePointType.START
            ),
            org.solidcoder.trackchecker.routes.domain.model.RoutePoint(
                org.solidcoder.trackchecker.routes.domain.model.RoutePointId("finish"),
                "Finish",
                org.solidcoder.trackchecker.routes.domain.model.Coordinates(1.0, 1.0),
                org.solidcoder.trackchecker.routes.domain.model.RoutePointType.FINISH
            )
        ),
        1L
    )
}
