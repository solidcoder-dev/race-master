package org.picolobruno.racing.rally.api.requests

import java.time.LocalDate

data class CreateRallyRequest(
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate
)
