package org.picolobruno.racing.rally.api

import org.picolobruno.racing.rally.api.dtos.RallyDto
import org.picolobruno.racing.rally.api.requests.CreateRallyRequest
import org.picolobruno.racing.rally.application.mappers.toCommand
import org.picolobruno.racing.rally.application.usecases.CreateRallyUseCase
import org.picolobruno.racing.rally.application.usecases.ListRalliesUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rallies")
class RallyController(
    private val createRally: CreateRallyUseCase,
    private val listRallies: ListRalliesUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateRallyRequest): RallyDto {
        val command = request.toCommand()
        return createRally.execute(command)
    }

    @GetMapping
    fun list(): List<RallyDto> = listRallies.execute()
}
