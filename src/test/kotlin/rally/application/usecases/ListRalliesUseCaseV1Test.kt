package rally.application.usecases

import io.mockk.every
import io.mockk.mockk
import org.instancio.Instancio
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.picolobruno.racing.rally.application.mappers.toDto
import org.picolobruno.racing.rally.application.usecases.ListRalliesResult
import org.picolobruno.racing.rally.application.usecases.ListRalliesUseCaseV1
import org.picolobruno.racing.rally.domain.objects.Rally
import org.picolobruno.racing.rally.domain.repositories.FindAllRallies

class ListRalliesUseCaseV1Test {

    private val rallyFinder = mockk<FindAllRallies>()
    private val useCase = ListRalliesUseCaseV1(rallyFinder)

    @Test
    fun `should return Success when repository returns rallies`() {
        val rallies = Instancio.ofList(Rally::class.java).size(3).create()
        every { rallyFinder.findAll() } returns Result.success(rallies)

        val result = useCase.execute()

        assertTrue(result is ListRalliesResult.Success)
        result as ListRalliesResult.Success
        assertEquals(3, result.rallies.size)
        assertEquals(rallies.map { it.toDto() }, result.rallies)
    }

    @Test
    fun `should return RepositoryFailure when repository returns error`() {
        every { rallyFinder.findAll() } returns Result.failure(Throwable("DB failure"))

        val result = useCase.execute()

        assertTrue(result is ListRalliesResult.RepositoryFailure)
        result as ListRalliesResult.RepositoryFailure
        assertEquals("Failed to load rallies", result.message)
    }
}
