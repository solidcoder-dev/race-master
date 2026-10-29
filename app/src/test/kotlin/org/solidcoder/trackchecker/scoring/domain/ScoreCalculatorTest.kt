package org.solidcoder.trackchecker.scoring.domain

import org.junit.Assert.*
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.scoring.domain.model.*
import org.solidcoder.trackchecker.scoring.domain.services.ScoreCalculator

class ScoreCalculatorTest {
    private val points = listOf(
        point("start", RoutePointType.START),
        point("wp", RoutePointType.WAYPOINT),
        point("wpe", RoutePointType.ECLIPSE_WAYPOINT),
        point("cp", RoutePointType.CHECKPOINT),
        point("finish", RoutePointType.FINISH)
    )

    @Test fun `mixed rules reward passed waypoint and penalize missed checkpoint`() {
        val route = route(ScoringPattern.MIXED)
        val result = ScoreCalculator().calculate(route, report(
            "wp" to PassageStatus.PASSED, "cp" to PassageStatus.MISSED
        ))!!
        assertEquals(-10, result.total)
        assertEquals(listOf(10, -20), result.contributions.map { it.points })
    }

    @Test fun `individual overrides affect the score without changing its sign`() {
        val route = route(ScoringPattern.MIXED).overridePointScore(RoutePointId("cp"), 35)
        val result = ScoreCalculator().calculate(route, report(
            "wp" to PassageStatus.PASSED, "cp" to PassageStatus.MISSED
        ))!!
        assertEquals(-25, result.total)
    }

    @Test fun `unconfigured scoring is pending rather than zero`() {
        assertNull(ScoreCalculator().calculate(route(null), report()))
    }

    @Test fun `uncertain passage contributes neither reward nor penalty`() {
        val result = ScoreCalculator().calculate(route(ScoringPattern.MIXED), report(
            "wp" to PassageStatus.UNCERTAIN, "cp" to PassageStatus.UNCERTAIN
        ))!!
        assertEquals(0, result.total)
    }

    private fun route(pattern: ScoringPattern?) = Route.imported(
        RouteId("r"), "Route", points, 0L, scoringPattern = pattern
    )

    private fun report(vararg statuses: Pair<String, PassageStatus>): TrackPassageReport {
        val byName = statuses.toMap()
        return TrackPassageReport(points.map {
            PointPassage(it.id, byName[it.id.value] ?: PassageStatus.PASSED, 0.0, 50.0)
        }, validStart = true, validFinish = true)
    }

    private fun point(id: String, type: RoutePointType) =
        RoutePoint(RoutePointId(id), id, Coordinates(28.0, -15.0), type)
}
