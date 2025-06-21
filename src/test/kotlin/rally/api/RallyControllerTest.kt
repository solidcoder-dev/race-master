package rally.api

import org.instancio.Instancio
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.picolobruno.racing.rally.api.RallyController
import org.picolobruno.racing.rally.api.RallyResponse
import org.picolobruno.racing.rally.api.dtos.RallyDto
import org.picolobruno.racing.rally.api.requests.CreateRallyRequest
import org.picolobruno.racing.rally.application.commands.CreateRallyCommand
import org.picolobruno.racing.rally.application.usecases.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import kotlin.test.Test

class RallyControllerTest {

    @Nested
    inner class CreateRally {

        @Test
        fun `should return 201 when rally is created successfully`() {
            val request = createValidRequest()
            val dto = createValidDto()
            val useCase = FakeCreateRallyUseCase(CreateRallyResult.Success(dto))

            val controller = RallyController(useCase, DummyListRalliesUseCase())
            val response = controller.create(request)

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals(RallyResponse.RallyDtoResponse(dto), response.body)
        }

        @Test
        fun `should return 400 on invalid input`() {
            val request = createValidRequest()
            val error = "Start date must be before end date"
            val useCase = FakeCreateRallyUseCase(CreateRallyResult.InvalidInput(error))

            val controller = RallyController(useCase, DummyListRalliesUseCase())
            val response = controller.create(request)

            assertError(response, HttpStatus.BAD_REQUEST, error)
        }

        @Test
        fun `should return 500 on repository failure`() {
            val request = createValidRequest()
            val useCase = FakeCreateRallyUseCase(CreateRallyResult.RepositoryFailure)

            val controller = RallyController(useCase, DummyListRalliesUseCase())
            val response = controller.create(request)

            assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred. Please try again later.")
        }
    }

    @Nested
    inner class ListRallies {

        @Test
        fun `should return 200 with list of rallies`() {
            val rallies = Instancio.ofList(RallyDto::class.java).size(3).create()
            val useCase = FakeListRalliesUseCase(ListRalliesResult.Success(rallies))

            val controller = RallyController(DummyCreateRallyUseCase(), useCase)
            val response = controller.list()

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(RallyResponse.ListRallyDtoResponse(rallies), response.body)
        }

        @Test
        fun `should return 500 when listing fails`() {
            val useCase = FakeListRalliesUseCase(ListRalliesResult.RepositoryFailure("db down"))

            val controller = RallyController(DummyCreateRallyUseCase(), useCase)
            val response = controller.list()

            assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred. Please try again later.")
        }
    }

    // Manual stubbing because mockk produces "org.objenesis.ObjenesisException" with sealed classes
    // TODO fix it
    private class FakeCreateRallyUseCase(
        private val result: CreateRallyResult
    ) : CreateRallyUseCase {
        override fun execute(command: CreateRallyCommand): CreateRallyResult = result
    }

    private class FakeListRalliesUseCase(
        private val result: ListRalliesResult
    ) : ListRalliesUseCase {
        override fun execute(): ListRalliesResult = result
    }

    private class DummyCreateRallyUseCase : CreateRallyUseCase {
        override fun execute(command: CreateRallyCommand): CreateRallyResult {
            throw UnsupportedOperationException("Not used in this test")
        }
    }

    private class DummyListRalliesUseCase : ListRalliesUseCase {
        override fun execute(): ListRalliesResult {
            throw UnsupportedOperationException("Not used in this test")
        }
    }

    private fun createValidRequest(): CreateRallyRequest =
        Instancio.create(CreateRallyRequest::class.java)

    private fun createValidDto(): RallyDto =
        Instancio.create(RallyDto::class.java)

    private fun assertError(
        response: ResponseEntity<RallyResponse>,
        expectedStatus: HttpStatus,
        expectedMessage: String
    ) {
        assertEquals(expectedStatus, response.statusCode)
        val body = response.body
        assertTrue(body is RallyResponse.ErrorResponse)
        val message = (body as RallyResponse.ErrorResponse).error
        assertEquals(expectedMessage, message)
    }
}
