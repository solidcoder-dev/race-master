package org.picolobruno.racing.infrastructure.persistance.repository

import org.picolobruno.racing.infrastructure.persistance.entities.RaceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaRaceRepository : JpaRepository<RaceEntity, String>
