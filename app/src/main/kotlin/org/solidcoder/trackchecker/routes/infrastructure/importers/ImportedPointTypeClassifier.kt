package org.solidcoder.trackchecker.routes.infrastructure.importers

import java.util.Locale
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType

/** Translates recognizable external labels into an explicit route point type. */
internal object ImportedPointTypeClassifier {
    private val aliases = mapOf(
        "SALIDA" to RoutePointType.START, "START" to RoutePointType.START,
        "META" to RoutePointType.FINISH, "FINISH" to RoutePointType.FINISH,
        "LLEGADA" to RoutePointType.FINISH,
        "WPE" to RoutePointType.ECLIPSE_WAYPOINT, "ECLIPSE" to RoutePointType.ECLIPSE_WAYPOINT,
        "CP" to RoutePointType.CHECKPOINT, "CHECKPOINT" to RoutePointType.CHECKPOINT,
        "WP" to RoutePointType.WAYPOINT, "WAYPOINT" to RoutePointType.WAYPOINT
    )
    private val explicitDescription = Regex("^(?:TYPE|TIPO)\\s*[:=]\\s*(.+)$")
    private val namePrefix = Regex("^(SALIDA|START|META|FINISH|LLEGADA|WPE|ECLIPSE|CHECKPOINT|CP|WAYPOINT|WP)(?=$|[0-9\\s_-])")

    fun classify(name: String?, description: String?, explicitType: String?): RoutePointType =
        classifyExactType(explicitType)
            ?: classifyDescription(description)
            ?: classifyName(name)
            ?: RoutePointType.OTHER

    private fun normalize(value: String?): String = value?.trim()?.uppercase(Locale.ROOT).orEmpty()

    private fun classifyExactType(value: String?): RoutePointType? = aliases[normalize(value)]

    private fun classifyDescription(value: String?): RoutePointType? {
        val label = normalize(value)
        return classifyExactType(label)
            ?: explicitDescription.matchEntire(label)?.groupValues?.get(1)?.let(::classifyExactType)
    }

    private fun classifyName(value: String?): RoutePointType? =
        namePrefix.find(normalize(value))?.groupValues?.get(1)?.let(aliases::get)
}
