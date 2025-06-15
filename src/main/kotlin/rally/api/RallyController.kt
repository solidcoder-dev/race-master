package org.picolobruno.racing.rally.api

import org.picolobruno.racing.rally.api.dtos.RallyDto
import org.picolobruno.racing.rally.api.requests.CreateRallyRequest
import org.picolobruno.racing.rally.application.mappers.toCommand
import org.picolobruno.racing.rally.application.usecases.CreateRallyResult
import org.picolobruno.racing.rally.application.usecases.CreateRallyUseCase
import org.picolobruno.racing.rally.application.usecases.ListRalliesResult
import org.picolobruno.racing.rally.application.usecases.ListRalliesUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

sealed interface RallyResponse {
    data class RallyDtoResponse(val dto: RallyDto) : RallyResponse
    data class ErrorResponse(val error: String) : RallyResponse
    data class ListRallyDtoResponse(val items: List<RallyDto>) : RallyResponse
}

@RestController
@RequestMapping("/rallies")
class RallyController(
    private val createRally: CreateRallyUseCase,
    private val listRallies: ListRalliesUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateRallyRequest): ResponseEntity<RallyResponse> {
        val command = request.toCommand()
        return when (val result = createRally.execute(command)) {
            is CreateRallyResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(RallyResponse.RallyDtoResponse(result.dto))

            is CreateRallyResult.InvalidInput ->
                ResponseEntity.badRequest().body(RallyResponse.ErrorResponse(result.reason))

            is CreateRallyResult.RepositoryFailure ->
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RallyResponse.ErrorResponse("An internal error occurred. Please try again later."))
        }
    }

    @GetMapping
    fun list(): ResponseEntity<RallyResponse> =
        when (val result = listRallies.execute()) {
            is ListRalliesResult.Success ->
                ResponseEntity.ok(RallyResponse.ListRallyDtoResponse(result.rallies))

            is ListRalliesResult.RepositoryFailure ->
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RallyResponse.ErrorResponse("An internal error occurred. Please try again later."))
        }
}
