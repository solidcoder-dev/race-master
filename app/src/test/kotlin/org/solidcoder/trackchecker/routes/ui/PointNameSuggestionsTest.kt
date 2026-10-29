package org.solidcoder.trackchecker.routes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.solidcoder.trackchecker.routes.ui.components.PointNameSuggestions

class PointNameSuggestionsTest {
    private fun point(id: String, name: String) =
        RoutePoint(RoutePointId(id), name, Coordinates(28.0, -15.0), RoutePointType.WAYPOINT)

    @Test fun duplicate_point_suggestion_does_not_renumber_neighbors() {
        val points = listOf(point("a", "WP04"), point("b", "WP04"), point("c", "WP05"))
        assertEquals("WP04B", PointNameSuggestions.forPoint(points, RoutePointId("b")))
        assertEquals(listOf("WP04", "WP04", "WP05"), points.map { it.name })
    }

    @Test fun suggestion_avoids_existing_names_regardless_of_case() {
        val points = listOf(point("a", "WP04"), point("b", "wp04"), point("c", "WP04B"))
        assertEquals("wp04C", PointNameSuggestions.forPoint(points, RoutePointId("b")))
    }

    @Test fun unique_and_unknown_points_need_no_rename_suggestion() {
        val points = listOf(point("a", "WP04"), point("b", "WP05"))
        assertNull(PointNameSuggestions.forPoint(points, RoutePointId("a")))
        assertNull(PointNameSuggestions.forPoint(points, RoutePointId("missing")))
    }
}
