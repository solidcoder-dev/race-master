package org.solidcoder.trackchecker.routes.domain

import org.junit.Assert.*
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.*

class RouteTest {
    private val start = RoutePoint(RoutePointId("start"), "Salida", Coordinates(28.0, -15.0), RoutePointType.START)
    private val waypoint = RoutePoint(RoutePointId("wp"), "WP04", Coordinates(28.1, -15.1), RoutePointType.WAYPOINT)
    private val duplicate = RoutePoint(RoutePointId("wp2"), "WP04", Coordinates(28.2, -15.2), RoutePointType.WAYPOINT)
    private val checkpoint = RoutePoint(RoutePointId("cp"), "CP1", Coordinates(28.25, -15.25), RoutePointType.CHECKPOINT)
    private val finish = RoutePoint(RoutePointId("finish"), "Meta", Coordinates(28.3, -15.3), RoutePointType.FINISH)

    private fun route() = Route.imported(RouteId("r1"), "Etapa 1", listOf(start, waypoint, duplicate, finish), 100L)

    @Test fun `duplicate names do not collapse distinct points`() {
        assertEquals(4, route().points.size)
        assertNotEquals(route().points[1].id, route().points[2].id)
    }

    @Test fun `renaming a point preserves its identity and the original route`() {
        val original = route()
        val renamed = original.renamePoint(waypoint.id, "Barranco")
        assertEquals("WP04", original.points[1].name)
        assertEquals("Barranco", renamed.points[1].name)
        assertEquals(waypoint.id, renamed.points[1].id)
        assertEquals(duplicate, renamed.points[2])
    }

    @Test fun `editing a ready route requires revalidation`() {
        val ready = Route.imported(RouteId("ready"), "Etapa", listOf(start, waypoint, checkpoint, finish), 100L).markReady()
        assertEquals(RouteStatus.READY, ready.status)
        assertEquals(RouteStatus.DRAFT, ready.changePointType(waypoint.id, RoutePointType.CHECKPOINT).status)
    }

    @Test fun `a route without start or finish cannot become ready`() {
        val invalid = Route.imported(RouteId("invalid"), "Etapa", listOf(waypoint), 100L)
        assertThrows(IllegalArgumentException::class.java) { invalid.markReady() }
    }

    @Test fun `readiness assessment reports errors without creating a ready route`() {
        val invalid = Route.imported(RouteId("invalid"), "Etapa", listOf(waypoint), 100L)
        val assessment = invalid.assessReadiness()
        assertFalse(assessment.validation.canBeReady)
        assertNull(assessment.readyRoute)
        assertEquals(RouteStatus.DRAFT, invalid.status)
    }

    @Test fun `readiness assessment returns a ready route and its report together`() {
        val original = Route.imported(RouteId("valid"), "Etapa", listOf(start, waypoint, checkpoint, finish), 100L)
        val assessment = original.assessReadiness()
        assertTrue(assessment.validation.canBeReady)
        assertEquals(RouteStatus.READY, assessment.readyRoute?.status)
        assertEquals(RouteStatus.DRAFT, original.status)
    }

    @Test fun `a route with multiple starts cannot become ready`() {
        val extraStart = start.copy(id = RoutePointId("extra"))
        val invalid = Route.imported(RouteId("invalid"), "Etapa", listOf(start, extraStart, finish), 100L)
        assertThrows(IllegalArgumentException::class.java) { invalid.markReady() }
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
