package org.picolobruno.racing.rally.domain.objects

import java.time.LocalDate
import java.util.UUID

data class Rally(
    val id: UUID,
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate
)