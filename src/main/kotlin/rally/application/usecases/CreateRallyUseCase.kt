package org.picolobruno.racing.rally.application.usecases

import org.picolobruno.racing.rally.api.dtos.RallyDto
import org.picolobruno.racing.rally.application.commands.CreateRallyCommand
import org.picolobruno.racing.rally.application.mappers.toDto
import org.picolobruno.racing.rally.domain.objects.Rally
import org.picolobruno.racing.rally.domain.repositories.RallyRepository
import org.springframework.stereotype.Component

sealed interface CreateRallyResult {
    data class Success(val dto: RallyDto) : CreateRallyResult
    data class InvalidInput(val reason: String) : CreateRallyResult
    object RepositoryFailure : CreateRallyResult
}

interface CreateRallyUseCase {
    fun execute(command: CreateRallyCommand): CreateRallyResult
}

@Component
class CreateRallyUseCaseV1(
    private val repository: RallyRepository
) : CreateRallyUseCase {

    override fun execute(command: CreateRallyCommand): CreateRallyResult {
        val rallyResult = Rally.new(
            command.name,
            command.description,
            command.startDate,
            command.endDate
        )

        return rallyResult.fold(::persist, ::invalidInput)
    }

    private fun persist(rally: Rally): CreateRallyResult =
        repository.save(rally).fold(
            onSuccess = { CreateRallyResult.Success(it.toDto()) },
            onFailure = { CreateRallyResult.RepositoryFailure }
        )

    private fun invalidInput(error: Throwable): CreateRallyResult =
        CreateRallyResult.InvalidInput(error.message ?: "Invalid rally data")
}
