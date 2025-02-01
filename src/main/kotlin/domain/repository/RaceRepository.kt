package org.picolobruno.racing.domain.repository

import org.picolobruno.racing.domain.race.Race

interface RaceRepository {
    fun findRaceById(raceId: String): Race
}