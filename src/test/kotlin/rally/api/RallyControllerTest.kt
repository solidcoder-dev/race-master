package rally.api

import io.mockk.every
import io.mockk.mockk
import org.instancio.Instancio
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.picolobruno.racing.rally.api.RallyController
import org.picolobruno.racing.rally.api.RallyResponse
import org.picolobruno.racing.rally.api.dtos.RallyDto
import org.picolobruno.racing.rally.api.requests.CreateRallyRequest
import org.picolobruno.racing.rally.application.usecases.CreateRallyResult
import org.picolobruno.racing.rally.application.usecases.CreateRallyUseCase
import org.picolobruno.racing.rally.application.usecases.ListRalliesResult
import org.picolobruno.racing.rally.application.usecases.ListRalliesUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import kotlin.test.Test

class RallyControllerTest {

    private val createRally = mockk<CreateRallyUseCase>()
    private val listRallies = mockk<ListRalliesUseCase>()
    private val controller = RallyController(createRally, listRallies)

    @Nested
    inner class CreateRally {

        @Test
        fun `should return 201 when rally is created successfully`() {
            val request = validRequest()
            val dto = Instancio.create(RallyDto::class.java)
            every { createRally.execute(any()) } returns CreateRallyResult.Success(dto)

            val response = controller.create(request)

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals(RallyResponse.RallyDtoResponse(dto), response.body)
        }

        @Test
        fun `should return 400 on invalid input`() {
            val request = validRequest()
            val error = "Start date must be before end date"
            every { createRally.execute(any()) } returns CreateRallyResult.InvalidInput(error)

            val response = controller.create(request)

            assertError(response, HttpStatus.BAD_REQUEST, error)
        }

        @Test
        fun `should return 500 on repository failure`() {
            val request = validRequest()
            every { createRally.execute(any()) } returns CreateRallyResult.RepositoryFailure

            val response = controller.create(request)

            assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred. Please try again later.")
        }
    }

    @Nested
    inner class ListRallies {

        @Test
        fun `should return 200 with list of rallies`() {
            val rallies = Instancio.ofList(RallyDto::class.java).size(3).create()
            every { listRallies.execute() } returns ListRalliesResult.Success(rallies)

            val response = controller.list()

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(RallyResponse.ListRallyDtoResponse(rallies), response.body)
        }

        @Test
        fun `should return 500 when listing fails`() {
            every { listRallies.execute() } returns ListRalliesResult.RepositoryFailure("db down")

            val response = controller.list()

            assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred. Please try again later.")
        }
    }

    private fun validRequest(): CreateRallyRequest =
        Instancio.create(CreateRallyRequest::class.java)

    private fun assertError(response: ResponseEntity<RallyResponse>, status: HttpStatus, message: String) {
        assertEquals(status, response.statusCode)
        assertTrue(response.body is RallyResponse.ErrorResponse)
        assertEquals(message, (response.body as RallyResponse.ErrorResponse).error)
    }
}
