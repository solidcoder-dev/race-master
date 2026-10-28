package org.solidcoder.trackchecker.routes.ui

import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteIssue

data class RoutesUiState(
    val routes: List<Route> = emptyList(),
    val importing: Boolean = false,
    val error: String? = null
)

/** A missing route is not a loading state (for example, after deletion). */
sealed interface RouteReviewUiState {
    data object Loading : RouteReviewUiState
    data class Loaded(val route: Route, val issues: List<RouteIssue>) : RouteReviewUiState
    data object NotFound : RouteReviewUiState
    data class Failed(val message: String) : RouteReviewUiState
}
