package org.picolobruno.racing.infrastructure.persistance.repository

import org.picolobruno.racing.domain.race.Race
import org.picolobruno.racing.domain.repository.RaceRepository

class RaceRepositoryV1(
    private val jpaRaceRepository: JpaRaceRepository
) : RaceRepository {
    override fun findRaceById(raceId: String): Race {
        TODO("Not yet implemented")
         return jpaRaceRepository.findById(raceId)
    }
}