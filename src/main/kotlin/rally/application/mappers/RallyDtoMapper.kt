package org.picolobruno.racing.rally.application.mappers

import org.picolobruno.racing.rally.api.dtos.RallyDto
import org.picolobruno.racing.rally.domain.objects.Rally


fun Rally.toDto() = RallyDto(
    id = id,
    name = name,
    description = description,
    startDate = startDate,
    endDate = endDate
)
