package org.picolobruno.racing.rally.application.usecases

import org.picolobruno.racing.rally.api.dtos.RallyDto
import org.picolobruno.racing.rally.application.mappers.toDto
import org.picolobruno.racing.rally.domain.repositories.RallyRepository

interface ListRalliesUseCase {
    fun execute(): List<RallyDto>
}

class ListRalliesUseCaseV1(
    private val repository: RallyRepository
) : ListRalliesUseCase {
    override fun execute(): List<RallyDto> = repository.findAll().map { it.toDto() }
}
