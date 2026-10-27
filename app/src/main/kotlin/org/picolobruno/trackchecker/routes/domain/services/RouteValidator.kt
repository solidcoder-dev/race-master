package org.picolobruno.trackchecker.routes.domain.services

import org.picolobruno.trackchecker.routes.domain.model.IssueSeverity
import org.picolobruno.trackchecker.routes.domain.model.Route
import org.picolobruno.trackchecker.routes.domain.model.RouteIssue
import org.picolobruno.trackchecker.routes.domain.model.RouteIssueCode
import org.picolobruno.trackchecker.routes.domain.model.RoutePointType
import org.picolobruno.trackchecker.routes.domain.model.RouteValidation

class RouteValidator {
    fun validate(route: Route): RouteValidation {
        val issues = mutableListOf<RouteIssue>()
        if (route.points.isEmpty()) {
            issues += RouteIssue(RouteIssueCode.NO_POINTS, IssueSeverity.ERROR, "La ruta no contiene puntos")
        }
        val starts = route.points.filter { it.type == RoutePointType.START }
        val finishes = route.points.filter { it.type == RoutePointType.FINISH }
        when (starts.size) {
            0 -> issues += RouteIssue(RouteIssueCode.MISSING_START, IssueSeverity.ERROR, "Identifica la salida")
            1 -> Unit
            else -> issues += RouteIssue(RouteIssueCode.MULTIPLE_START, IssueSeverity.ERROR, "Hay varias salidas", starts.map { it.id })
        }
        when (finishes.size) {
            0 -> issues += RouteIssue(RouteIssueCode.MISSING_FINISH, IssueSeverity.ERROR, "Identifica la meta")
            1 -> Unit
            else -> issues += RouteIssue(RouteIssueCode.MULTIPLE_FINISH, IssueSeverity.ERROR, "Hay varias metas", finishes.map { it.id })
        }
        route.points.groupBy { it.name.trim().lowercase() }
            .filter { it.value.size > 1 }
            .values.forEach { points ->
                issues += RouteIssue(
                    RouteIssueCode.DUPLICATE_NAME, IssueSeverity.WARNING,
                    "Nombre repetido: ${points.first().name}",
                    points.map { it.id }
                )
            }
        return RouteValidation(issues)
    }
}
