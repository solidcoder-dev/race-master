package org.solidcoder.trackchecker.routes.ui

import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteIssue
import org.solidcoder.trackchecker.scoring.application.usecases.EvaluatedTrack
import org.solidcoder.trackchecker.tracks.domain.model.TrackId

data class RoutesUiState(
    val routes: List<Route> = emptyList(),
    val importing: Boolean = false,
)

sealed interface RoutesUiEvent {
    data class Message(val text: String, val isError: Boolean = false) : RoutesUiEvent
}

/** A missing route is not a loading state (for example, after deletion). */
sealed interface RouteReviewUiState {
    data object Loading : RouteReviewUiState
    data class Loaded(
        val route: Route,
        val issues: List<RouteIssue>,
        val tracks: List<EvaluatedTrack> = emptyList(),
        val selectedTrackId: TrackId? = null,
        val importingTrack: Boolean = false
    ) : RouteReviewUiState
    data object NotFound : RouteReviewUiState
    data class Failed(val message: String) : RouteReviewUiState
}
