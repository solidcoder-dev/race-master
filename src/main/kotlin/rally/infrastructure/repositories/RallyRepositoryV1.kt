package org.picolobruno.racing.rally.infrastructure.repositories

import org.picolobruno.racing.rally.domain.objects.Rally
import org.picolobruno.racing.rally.domain.repositories.FindAllRallies
import org.picolobruno.racing.rally.domain.repositories.SaveRally
import org.picolobruno.racing.rally.infrastructure.entities.RallyEntity
import org.picolobruno.racing.rally.infrastructure.entities.toDomain
import org.picolobruno.racing.rally.infrastructure.entities.toEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class RallyRepositoryV1(
    private val jpa: JpaRepository<RallyEntity, UUID>,
): SaveRally, FindAllRallies {
    override fun save(rally: Rally) : Result<Rally> = runCatching {
        jpa.save(rally.toEntity()).toDomain()
    }

    override fun findAll(): Result<List<Rally>> = runCatching {
        jpa.findAll().map { it.toDomain() }
    }
}
