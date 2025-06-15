package org.picolobruno.racing.rally.application.usecases

import org.picolobruno.racing.rally.api.dtos.RallyDto
import org.picolobruno.racing.rally.application.commands.CreateRallyCommand
import org.picolobruno.racing.rally.application.mappers.toDto
import org.picolobruno.racing.rally.domain.objects.Rally
import org.picolobruno.racing.rally.domain.repositories.RallyRepository
import org.springframework.stereotype.Component
import java.util.*

interface CreateRallyUseCase {
    fun execute(command: CreateRallyCommand): RallyDto
}

@Component
class CreateRallyUseCaseV1(
    private val repository: RallyRepository
) : CreateRallyUseCase {
    override fun execute(command: CreateRallyCommand): RallyDto {
        require(command.startDate.isBefore(command.endDate)) {
            "Start date must be before end date"
        }

        val rally = Rally(
            id = UUID.randomUUID(),
            name = command.name,
            description = command.description,
            startDate = command.startDate,
            endDate = command.endDate
        )
        val result = repository.save(rally)
        return result.toDto()
    }
}
