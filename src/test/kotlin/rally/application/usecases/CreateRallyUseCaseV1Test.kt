package rally.application.usecases

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.picolobruno.racing.rally.application.commands.CreateRallyCommand
import org.picolobruno.racing.rally.application.mappers.toDto
import org.picolobruno.racing.rally.application.usecases.CreateRallyResult
import org.picolobruno.racing.rally.application.usecases.CreateRallyUseCaseV1
import org.picolobruno.racing.rally.domain.objects.Rally
import org.picolobruno.racing.rally.domain.repositories.SaveRally
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateRallyUseCaseV1Test {

    private val repository = mockk<SaveRally>(relaxed = true)
    private val useCase = CreateRallyUseCaseV1(repository)

    @Test
    fun `returns Success when rally is valid and saved`() {
        val command = validCommand()
        val rally = Rally(UUID.randomUUID(), command.name, command.description, command.startDate, command.endDate)
        val expectedDto = rally.toDto()

        every { repository.save(any()) } returns Result.success(rally)

        val result = useCase.execute(command)

        assertIs<CreateRallyResult.Success>(result)
        assertEquals(expectedDto, result.dto)
    }

    @Test
    fun `returns InvalidInput when rally dates are invalid`() {
        val command = CreateRallyCommand(
            name = "Bad Rally",
            description = "bad date order",
            startDate = LocalDate.of(2025, 1, 10),
            endDate = LocalDate.of(2025, 1, 5)
        )

        val result = useCase.execute(command)

        assertIs<CreateRallyResult.InvalidInput>(result)
        assertEquals("Start date must be before end date", result.reason)
    }

    @Test
    fun `returns RepositoryFailure when save fails`() {
        val command = validCommand()

        every { repository.save(any()) } returns Result.failure(RuntimeException("DB failure"))

        val result = useCase.execute(command)

        assertIs<CreateRallyResult.RepositoryFailure>(result)
    }

    private fun validCommand(): CreateRallyCommand =
        CreateRallyCommand(
            name = "My Rally",
            description = "A description",
            startDate = LocalDate.of(2025, 1, 1),
            endDate = LocalDate.of(2025, 1, 10)
        )
}
