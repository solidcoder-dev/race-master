package org.picolobruno.racing.rally.infrastructure.repositories

import org.picolobruno.racing.rally.domain.objects.Rally
import org.picolobruno.racing.rally.domain.repositories.RallyRepository
import org.picolobruno.racing.rally.infrastructure.entities.RallyEntity
import org.picolobruno.racing.rally.infrastructure.entities.toDomain
import org.picolobruno.racing.rally.infrastructure.entities.toEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RallyRepositoryV1(
    private val jpa: JpaRepository<RallyEntity, UUID>,
): RallyRepository {
    override fun save(rally: Rally) : Rally =
        jpa.save(rally.toEntity()).toDomain()

    override fun findAll(): List<Rally> =
        jpa.findAll().map { it.toDomain() }
}
