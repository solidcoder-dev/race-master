package org.picolobruno.racing.rally.domain.objects

import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.util.UUID

data class Rally(
    val id: UUID,
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    companion object {
        fun new(
            name: String,
            description: String?,
            startDate: LocalDate,
            endDate: LocalDate
        ): Result<Rally> {
            if (!startDate.isBefore(endDate)) {
                return Result.failure(IllegalArgumentException("Start date must be before end date"))
            }

            return Result.success(
                Rally(
                    id = UUID.randomUUID(),
                    name = name,
                    description = description,
                    startDate = startDate,
                    endDate = endDate
                )
            )
        }
    }
}
