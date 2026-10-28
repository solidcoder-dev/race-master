package org.solidcoder.trackchecker.routes.ui

import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteIssue

data class RoutesUiState(
    val routes: List<Route> = emptyList(),
    val importing: Boolean = false,
    val error: String? = null
)

data class RouteReviewUiState(
    val route: Route? = null,
    val issues: List<RouteIssue> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)
