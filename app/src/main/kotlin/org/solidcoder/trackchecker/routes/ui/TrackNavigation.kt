package org.solidcoder.trackchecker.routes.ui

import androidx.compose.runtime.saveable.Saver

enum class TrackDestination { ROUTES, ROUTE_REVIEW, TRACKS, TRACK_DETAIL, TRACK_MAP }

data class TrackNavigationState(
    val destination: TrackDestination,
    val routeId: String? = null,
    val trackId: String? = null
) {
    init {
        require(destination == TrackDestination.ROUTES || routeId != null) {
            "A route is required outside the routes list"
        }
        require(destination in setOf(TrackDestination.TRACK_DETAIL, TrackDestination.TRACK_MAP) == (trackId != null)) {
            "A track is required only for track detail and map destinations"
        }
    }

    fun openRoute(route: String): TrackNavigationState =
        copy(destination = TrackDestination.ROUTE_REVIEW, routeId = route, trackId = null)

    fun openTracks(): TrackNavigationState = requireRoute().copy(destination = TrackDestination.TRACKS)

    fun openTrackDetail(track: String): TrackNavigationState =
        requireRoute().copy(destination = TrackDestination.TRACK_DETAIL, trackId = track)

    fun openTrackMap(): TrackNavigationState {
        require(destination == TrackDestination.TRACK_DETAIL) { "Map opens from track detail" }
        return copy(destination = TrackDestination.TRACK_MAP)
    }

    fun back(): TrackNavigationState = when (destination) {
        TrackDestination.ROUTES -> this
        TrackDestination.ROUTE_REVIEW -> TrackNavigationState(TrackDestination.ROUTES)
        TrackDestination.TRACKS -> copy(destination = TrackDestination.ROUTE_REVIEW, trackId = null)
        TrackDestination.TRACK_DETAIL -> copy(destination = TrackDestination.TRACKS, trackId = null)
        TrackDestination.TRACK_MAP -> copy(destination = TrackDestination.TRACK_DETAIL)
    }

    private fun requireRoute(): TrackNavigationState =
        requireNotNull(routeId) { "A route is required for track navigation" }.let { this }

    companion object {
        val Saver: Saver<TrackNavigationState, String> = Saver(
            save = { state -> listOf(state.destination.name, state.routeId ?: "", state.trackId ?: "").joinToString("|") },
            restore = { encoded ->
                val parts = encoded.split('|')
                TrackNavigationState(
                    destination = TrackDestination.valueOf(parts[0]),
                    routeId = parts.getOrNull(1)?.takeIf(String::isNotEmpty),
                    trackId = parts.getOrNull(2)?.takeIf(String::isNotEmpty)
                )
            }
        )
    }
}
