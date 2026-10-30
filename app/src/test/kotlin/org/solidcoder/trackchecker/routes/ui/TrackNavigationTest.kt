package org.solidcoder.trackchecker.routes.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackNavigationTest {
    @Test
    fun `navigation follows route review tracks detail map and back`() {
        val route = TrackNavigationState(TrackDestination.ROUTES).openRoute("route-1")

        val tracks = route.openTracks()
        val detail = tracks.openTrackDetail("track-1")
        val map = detail.openTrackMap()

        assertEquals(TrackNavigationState(TrackDestination.TRACK_DETAIL, "route-1", "track-1"), map.back())
        assertEquals(TrackNavigationState(TrackDestination.TRACKS, "route-1"), map.back().back())
        assertEquals(TrackNavigationState(TrackDestination.ROUTE_REVIEW, "route-1"), map.back().back().back())
        assertEquals(TrackNavigationState(TrackDestination.ROUTES), map.back().back().back().back())
    }

    @Test
    fun `map cannot be opened from route review`() {
        val route = TrackNavigationState(TrackDestination.ROUTES).openRoute("route-1")

        try {
            route.openTrackMap()
            throw AssertionError("Expected map transition to fail")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Map opens from track detail", expected.message)
        }
    }
}
