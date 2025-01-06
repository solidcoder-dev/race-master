package org.picolobruno.racing.application

import org.picolobruno.racing.domain.Score
import org.picolobruno.racing.domain.ScoringService
import org.picolobruno.racing.domain.Tolerance
import org.picolobruno.racing.infrastructure.KmlParser

class ScoreCalculator(
    private val scoringService: ScoringService
) {
    fun calculate(
        trackFilePath: String,
        raceRouteFilePath: String,
        waypointScores: Map<String, Score>,
        toleranceKm: Double
    ): Score =
        scoringService.calculateScore(
            track = KmlParser.parseTrack(trackFilePath),
            raceRoute = KmlParser.parseRaceRoute(raceRouteFilePath, waypointScores),
            tolerance = Tolerance(toleranceKm)
        )
}
