package org.solidcoder.trackchecker.scoring.domain.services

import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus
import org.solidcoder.trackchecker.scoring.domain.model.PointContribution
import org.solidcoder.trackchecker.scoring.domain.model.Score
import org.solidcoder.trackchecker.scoring.domain.model.TrackPassageReport

/** Scores geographical evidence once per route point, using the route's configured rules. */
class ScoreCalculator {
    fun calculate(route: Route, report: TrackPassageReport): Score? {
        if (route.scoringPattern == null) return null
        val evidence = report.passages.associateBy { it.pointId }
        val contributions = route.points.mapNotNull { point ->
            val effect = route.scoringFor(point.id) ?: return@mapNotNull null
            val passage = evidence[point.id] ?: error("Missing evidence for ${point.id.value}")
            val value = when (passage.status) {
                PassageStatus.PASSED -> effect.whenPassed
                PassageStatus.MISSED -> effect.whenMissed
                PassageStatus.UNCERTAIN -> 0
            }
            PointContribution(point.id, value)
        }
        return Score(contributions.sumOf { it.points }, contributions)
    }
}
