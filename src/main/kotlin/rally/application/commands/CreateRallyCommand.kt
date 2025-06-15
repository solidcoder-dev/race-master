package org.picolobruno.racing.rally.application.commands

import java.time.LocalDate

data class CreateRallyCommand(
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate
)
