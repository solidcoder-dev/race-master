package org.picolobruno.racing.rally.application.usecases

import org.picolobruno.racing.rally.api.dtos.RallyDto
import org.picolobruno.racing.rally.application.mappers.toDto
import org.picolobruno.racing.rally.domain.objects.Rally
import org.picolobruno.racing.rally.domain.repositories.FindAllRallies
import org.springframework.stereotype.Component

sealed interface ListRalliesResult {
    data class Success(val rallies: List<RallyDto>) : ListRalliesResult
    data class RepositoryFailure(val message: String) : ListRalliesResult
}

interface ListRalliesUseCase {
    fun execute(): ListRalliesResult
}

@Component
class ListRalliesUseCaseV1(
    private val rallyFinder: FindAllRallies
) : ListRalliesUseCase {

    override fun execute(): ListRalliesResult =
        rallyFinder.findAll()
            .fold(
                {ListRalliesResult.Success(it.map(Rally::toDto)) },
                { ListRalliesResult.RepositoryFailure("Failed to load rallies") }
            )

}
