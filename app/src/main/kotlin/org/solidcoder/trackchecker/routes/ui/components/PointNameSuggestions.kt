package org.solidcoder.trackchecker.routes.ui.components

import java.util.Locale
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId

/** Non-destructive naming suggestion for the point editor, not a route invariant. */
internal object PointNameSuggestions {
    fun forPoint(points: List<RoutePoint>, pointId: RoutePointId): String? {
        val point = points.firstOrNull { it.id == pointId } ?: return null
        val occupied = points.asSequence().filter { it.id != pointId }
            .map { it.name.trim().lowercase(Locale.ROOT) }.toSet()
        val base = point.name.trim()
        if (base.lowercase(Locale.ROOT) !in occupied) return null
        return ('B'..'Z').asSequence().map { "$base$it" }
            .plus(generateSequence(2) { it + 1 }.map { "$base-$it" })
            .first { it.lowercase(Locale.ROOT) !in occupied }
    }
}
