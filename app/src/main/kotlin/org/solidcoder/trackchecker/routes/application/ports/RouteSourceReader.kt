package org.solidcoder.trackchecker.routes.application.ports

import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType

/** Raw imported data; the domain does not depend on file formats or Android URIs. */
data class RouteImportSource(val fileName: String, val content: ByteArray)

data class ImportedPoint(
    val name: String?,
    val coordinates: Coordinates,
    val type: RoutePointType
)

/** Reads route definitions from an external source, regardless of format. */
fun interface RouteSourceReader {
    /** Throws an IllegalArgumentException for unsupported or malformed input. */
    fun read(source: RouteImportSource): List<ImportedPoint>
}
