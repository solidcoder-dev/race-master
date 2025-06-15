package org.picolobruno.racing.rally.infrastructure.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.picolobruno.racing.rally.domain.objects.Rally
import java.time.LocalDate
import java.util.UUID

@Entity(name = "rally")
data class RallyEntity(
    @Id
    val id: UUID,
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate
)

// --- Mappers ---
fun RallyEntity.toDomain(): Rally =
    Rally(id, name, description, startDate, endDate)

fun Rally.toEntity(): RallyEntity =
    RallyEntity(id, name, description, startDate, endDate)