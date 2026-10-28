package org.solidcoder.trackchecker.routes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.solidcoder.trackchecker.routes.domain.model.RouteStatus

class RouteTest {
    private val first = RoutePoint(RoutePointId("p1"), "WP04", Coordinates(28.0, -15.0), RoutePointType.WAYPOINT)
    private val second = RoutePoint(RoutePointId("p2"), "WP04", Coordinates(28.1, -15.1), RoutePointType.WAYPOINT)
    private fun route() = Route.imported(RouteId("r1"), "Etapa 1", listOf(first, second), 100L)

    @Test fun `duplicate names do not collapse distinct points`() {
        assertEquals(2, route().points.size)
        assertNotEquals(route().points[0].id, route().points[1].id)
    }

    @Test fun `renaming a point preserves its identity and the original route`() {
        val original = route()
        val renamed = original.renamePoint(first.id, "Barranco")
        assertEquals("WP04", original.points.first().name)
        assertEquals("Barranco", renamed.points.first().name)
        assertEquals(first.id, renamed.points.first().id)
        assertEquals(second, renamed.points.last())
    }

    @Test fun `editing a ready route requires revalidation`() {
        val ready = route().markReady(org.solidcoder.trackchecker.routes.domain.model.RouteValidation(emptyList()))
        assertEquals(RouteStatus.DRAFT, ready.changePointType(first.id, RoutePointType.START).status)
    }

    @Test fun `unknown point cannot be edited`() {
        assertThrows(IllegalArgumentException::class.java) {
            route().renamePoint(RoutePointId("missing"), "New")
        }
    }

    @Test fun `coordinates reject impossible positions`() {
        assertThrows(IllegalArgumentException::class.java) { Coordinates(91.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { Coordinates(Double.NaN, 0.0) }
    }
}
