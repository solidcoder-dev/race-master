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

    @Test fun `route points inherit the default detection radius`() {
        assertEquals(50.0, route().detectionRadiusFor(waypoint.id).meters, 0.0)
    }

    @Test fun `changing the route radius updates inherited values`() {
        val updated = route().changeDefaultDetectionRadius(DetectionRadius(80.0))
        assertEquals(80.0, updated.detectionRadiusFor(waypoint.id).meters, 0.0)
        assertEquals(50.0, route().detectionRadiusFor(waypoint.id).meters, 0.0)
    }

    @Test fun `point override survives route changes and can be reset to inheritance`() {
        val customized = route().overridePointDetectionRadius(waypoint.id, DetectionRadius(100.0))
        val resized = customized.changeDefaultDetectionRadius(DetectionRadius(75.0))
        assertEquals(100.0, resized.detectionRadiusFor(waypoint.id).meters, 0.0)
        assertEquals(75.0, resized.detectionRadiusFor(duplicate.id).meters, 0.0)
        val reset = resized.overridePointDetectionRadius(waypoint.id, null)
        assertEquals(75.0, reset.detectionRadiusFor(waypoint.id).meters, 0.0)
    }

    @Test fun `invalid detection radii are rejected`() {
        listOf(0.0, -10.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { DetectionRadius(value) }
        }
    }

    @Test fun `mixed scoring distinguishes passed waypoints and missed checkpoints`() {
        val configured = Route.imported(
            RouteId("scored"), "Etapa", listOf(start, waypoint, checkpoint, finish), 100L,
            scoringPattern = ScoringPattern.MIXED
        )
        assertEquals(PointScoreEffect(whenPassed = 10), configured.scoringFor(waypoint.id))
        assertEquals(PointScoreEffect(whenMissed = -20), configured.scoringFor(checkpoint.id))
        assertNull(configured.scoringFor(start.id))
        assertNull(configured.scoringFor(finish.id))
    }

    @Test fun `a point override changes magnitude but retains its type scoring behavior`() {
        val overridden = checkpoint.withScoreValueOverride(35)
        assertEquals(PointScoreEffect(whenMissed = -35),
            ScoringPattern.PASSED_POINTS.effectFor(overridden.type, overridden.scoreValueOverride))
        assertThrows(IllegalArgumentException::class.java) { waypoint.withScoreValueOverride(-1) }
    }

    @Test fun `unscored types never gain points from a custom value`() {
        val wpe = waypoint.changeType(RoutePointType.ECLIPSE_WAYPOINT).withScoreValueOverride(40)
        assertNull(ScoringPattern.MIXED.effectFor(wpe.type, wpe.scoreValueOverride))
    }

    @Test fun `applying a new pattern keeps individual scores unless explicitly replaced`() {
        val initial = route().overridePointScore(waypoint.id, 25)
            .applyScoringPattern(ScoringPattern.PASSED_POINTS)
        val changed = initial.applyScoringPattern(ScoringPattern.MIXED)
        assertEquals(PointScoreEffect(whenPassed = 25), changed.scoringFor(waypoint.id))
        assertEquals(PointScoreEffect(whenPassed = 10), changed.scoringFor(duplicate.id))
        val reset = changed.applyScoringPattern(ScoringPattern.MIXED, replaceOverrides = true)
        assertEquals(PointScoreEffect(whenPassed = 10), reset.scoringFor(waypoint.id))
        assertNull(reset.points.first { it.id == waypoint.id }.scoreValueOverride)
    }

    @Test fun `applying a scoring pattern never resets custom detection radii`() {
        val custom = route().overridePointDetectionRadius(waypoint.id, DetectionRadius(100.0))
        assertEquals(100.0,
            custom.applyScoringPattern(ScoringPattern.MIXED, replaceOverrides = true)
                .detectionRadiusFor(waypoint.id).meters, 0.0)
    }

    @Test fun `bulk scoring changes only the selected IDs and only requested fields`() {
        val original = route()
        val edited = original.updatePoints(setOf(waypoint.id, duplicate.id), PointChanges(
            score = OverrideUpdate.Set(25)
        ))
        assertEquals(25, edited.points[1].scoreValueOverride)
        assertEquals(25, edited.points[2].scoreValueOverride)
        assertEquals(waypoint.type, edited.points[1].type)
        assertNull(edited.points[1].detectionRadiusOverride)
        assertNull(edited.points[0].scoreValueOverride)
        assertNull(original.points[1].scoreValueOverride)
    }

    @Test fun `bulk radius reset does not change custom scores`() {
        val original = route().overridePointScore(waypoint.id, 40)
            .overridePointDetectionRadius(waypoint.id, DetectionRadius(100.0))
        val edited = original.updatePoints(setOf(waypoint.id), PointChanges(
            radius = OverrideUpdate.Set(null)
        ))
        assertEquals(40, edited.points[1].scoreValueOverride)
        assertEquals(50.0, edited.detectionRadiusFor(waypoint.id).meters, 0.0)
    }

    @Test fun `bulk edits may introduce validation issues but cannot mark an invalid route ready`() {
        val original = route()
        val edited = original.updatePoints(setOf(waypoint.id, duplicate.id), PointChanges(type = RoutePointType.START))
        assertFalse(edited.assessReadiness().validation.canBeReady)
        assertThrows(IllegalArgumentException::class.java) { edited.markReady() }
        assertEquals(RoutePointType.WAYPOINT, original.points[1].type)
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
