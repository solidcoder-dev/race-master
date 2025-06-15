package org.picolobruno.racing.rally.api.dtos

import java.time.LocalDate
import java.util.UUID

data class RallyDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate
)
