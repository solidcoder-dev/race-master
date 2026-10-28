package org.solidcoder.trackchecker.routes.application.ports

import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType

/** Parsed source data has no domain IDs. Every import creates fresh identities. */
data class ImportedPoint(
    val name: String?,
    val coordinates: Coordinates,
    val type: RoutePointType
)

interface RouteSourceParser {
    /** Throws a descriptive exception for malformed or unsupported files. */
    fun parse(fileName: String, content: ByteArray): List<ImportedPoint>
}
