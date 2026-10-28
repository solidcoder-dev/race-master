package org.solidcoder.trackchecker.routes.domain.services

import java.util.Locale
import org.solidcoder.trackchecker.routes.domain.model.IssueSeverity
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteIssue
import org.solidcoder.trackchecker.routes.domain.model.RouteIssueCode
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.solidcoder.trackchecker.routes.domain.model.RouteValidation

class RouteValidator {
    fun validate(route: Route): RouteValidation {
        val issues = mutableListOf<RouteIssue>()
        if (route.points.isEmpty()) {
            issues += RouteIssue(RouteIssueCode.NO_POINTS, IssueSeverity.ERROR)
        }
        val starts = route.points.filter { it.type == RoutePointType.START }
        val finishes = route.points.filter { it.type == RoutePointType.FINISH }
        when (starts.size) {
            0 -> issues += RouteIssue(RouteIssueCode.MISSING_START, IssueSeverity.ERROR)
            1 -> Unit
            else -> issues += RouteIssue(
                RouteIssueCode.MULTIPLE_START, IssueSeverity.ERROR, starts.map { it.id }
            )
        }
        when (finishes.size) {
            0 -> issues += RouteIssue(RouteIssueCode.MISSING_FINISH, IssueSeverity.ERROR)
            1 -> Unit
            else -> issues += RouteIssue(
                RouteIssueCode.MULTIPLE_FINISH, IssueSeverity.ERROR, finishes.map { it.id }
            )
        }
        route.points.groupBy { it.name.trim().lowercase(Locale.ROOT) }
            .filterValues { it.size > 1 }
            .values.forEach { points ->
                issues += RouteIssue(
                    RouteIssueCode.DUPLICATE_NAME, IssueSeverity.WARNING, points.map { it.id }
                )
            }
        return RouteValidation(issues)
    }
}
