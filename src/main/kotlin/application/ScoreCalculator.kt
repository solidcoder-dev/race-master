package org.picolobruno.racing.application

import org.picolobruno.racing.domain.Participant
import org.picolobruno.racing.domain.RaceRoute
import org.picolobruno.racing.domain.Score
import org.picolobruno.racing.domain.ScoringService
import org.picolobruno.racing.domain.Tolerance
import org.picolobruno.racing.kml.application.KmlParser

class ScoreCalculator(
    private val scoringService: ScoringService
) {
    // TODO: this is wrong because the participant is missing, right?
    // TODO: Could we combine some parameters?
    fun calculate(
        track: String,
        raceRoute: String,
        waypointScores: Map<String, Score>,
        toleranceKm: Double
    ): Score =
        scoringService.calculateScore(
            participant = Participant.from(KmlParser.parse(track)),
            raceRoute = RaceRoute.from(KmlParser.parse(raceRoute)),
            tolerance = Tolerance(toleranceKm)
        )

}
