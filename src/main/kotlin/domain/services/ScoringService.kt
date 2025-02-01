package org.picolobruno.racing.domain.services

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.picolobruno.racing.domain.participant.Score
import org.picolobruno.racing.domain.participant.Track
import org.picolobruno.racing.domain.participant.TrackPoint
import org.picolobruno.racing.domain.race.Race
import org.picolobruno.racing.domain.race.Racepoint
import org.picolobruno.racing.domain.race.Tolerance

class ScoringService {

    fun calculateScore(race: Race, track: Track): Score {
        if (!validateTrackStartAndEnd(track, race)) {
            return Score.Invalid(
                "Track doesn't start and finish by the correct coordinates"
            )
        }

        val score = Score.Defined()
        getPassedWayPoints(track, race).forEach { point ->
            score.addSuccess(
                value = race.scoringRules.get(point.name)!!, // TODO fix me
                waypoint = point
            )
        }
        getMissedCheckpoints(track, race).forEach { point ->
            score.addPenalty(
                value = race.scoringRules.get(point.name)!!, // TODO fix me
                checkpoint = point
            )
        }
        return score
    }

    private fun validateTrackStartAndEnd(track: Track, race: Race): Boolean {
        // TODO: we are supposing these are ordered...
        val trackStart = track.points.first()
        val trackEnd = track.points.last()

        return isWithinTolerance(trackStart, race.startPoint, race.tolerance) &&
            isWithinTolerance(trackEnd, race.endpoint, race.tolerance)
    }

    private fun isWithinTolerance(trackPoint: TrackPoint, racePoint: Racepoint, tolerance: Tolerance): Boolean {
        val distance = calculateDistance(trackPoint, racePoint)
        return distance <= tolerance.distanceTolerance
    }

    private fun calculateDistance(tp: TrackPoint, rp: Racepoint): Double {
        return haversine(tp.latitude, tp.longitude, rp.latitude, rp.longitude)
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
            cos(phi1) * cos(phi2) *
            sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    private fun getPassedWayPoints(track: Track, race: Race): List<Racepoint> {
        val passedPoints = mutableListOf<Racepoint>()
        for (waypoint in race.waypoints) {
            for (trackPoint in track.points) {
                if (isWithinTolerance(trackPoint, waypoint, race.tolerance)) {
                    passedPoints.add(waypoint)
                    break
                }
            }
        }
        return passedPoints
    }

    private fun getMissedCheckpoints(track: Track, race: Race): List<Racepoint> {
        return race.checkpoints.filterNot { checkpoint ->
            track.points.any { trackPoint ->
                isWithinTolerance(trackPoint, checkpoint, race.tolerance)
            }
        }
    }
}
