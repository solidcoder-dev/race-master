package org.picolobruno.racing.rally.application.mappers

import org.picolobruno.racing.rally.api.requests.CreateRallyRequest
import org.picolobruno.racing.rally.application.commands.CreateRallyCommand

fun CreateRallyRequest.toCommand(): CreateRallyCommand =
    CreateRallyCommand(
        name = this.name,
        description = this.description,
        startDate = this.startDate,
        endDate = this.endDate
    )
