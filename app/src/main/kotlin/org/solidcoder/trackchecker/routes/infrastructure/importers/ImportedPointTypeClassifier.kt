package org.solidcoder.trackchecker.routes.infrastructure.importers

import java.util.Locale
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType

/** Translates optional external labels into the route's explicit domain type. */
internal object ImportedPointTypeClassifier {
    fun classify(name: String?, description: String?, explicitType: String?): RoutePointType =
        listOf(explicitType, description, name).firstNotNullOfOrNull(::classifyLabel)
            ?: RoutePointType.OTHER

    private fun classifyLabel(value: String?): RoutePointType? {
        val label = value?.trim()?.uppercase(Locale.ROOT).orEmpty()
        val primary = label.split(Regex("[^A-Z0-9]+")).firstOrNull().orEmpty()
        return when {
            primary.startsWith("SALIDA") || primary.startsWith("START") -> RoutePointType.START
            primary.startsWith("META") || primary.startsWith("FINISH") || primary.startsWith("LLEGADA") -> RoutePointType.FINISH
            primary.startsWith("WPE") || primary == "ECLIPSE" -> RoutePointType.ECLIPSE_WAYPOINT
            primary.startsWith("CP") || primary == "CHECKPOINT" -> RoutePointType.CHECKPOINT
            primary.startsWith("WP") || primary == "WAYPOINT" -> RoutePointType.WAYPOINT
            else -> null
        }
    }
}
