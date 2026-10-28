package org.solidcoder.trackchecker.routes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.domain.services.RouteValidator

class RouteValidatorTest {
    private fun point(id: String, name: String, type: RoutePointType) =
        RoutePoint(RoutePointId(id), name, Coordinates(28.0, -15.0), type)

    @Test fun `duplicated point labels warn but do not prevent validation`() {
        val route = Route.imported(
            RouteId("r"), "Etapa", listOf(
                point("start", "Salida", RoutePointType.START),
                point("wp1", "WP04", RoutePointType.WAYPOINT),
                point("wp2", "WP04", RoutePointType.WAYPOINT),
                point("end", "Meta", RoutePointType.FINISH)
            ), 10
        )
        val result = RouteValidator().validate(route)
        assertTrue(result.canBeReady)
        assertEquals(RouteIssueCode.DUPLICATE_NAME, result.issues.single().code)
        assertEquals(listOf(RoutePointId("wp1"), RoutePointId("wp2")), result.issues.single().pointIds)
    }

    @Test fun `missing start and finish block validation`() {
        val route = Route.imported(RouteId("r"), "Etapa", listOf(point("wp", "WP1", RoutePointType.WAYPOINT)), 10)
        val result = RouteValidator().validate(route)
        assertFalse(result.canBeReady)
        assertTrue(result.issues.any { it.code == RouteIssueCode.MISSING_START })
        assertTrue(result.issues.any { it.code == RouteIssueCode.MISSING_FINISH })
    }

    @Test fun `one start and one finish without duplicated names is ready`() {
        val route = Route.imported(
            RouteId("r"), "Etapa", listOf(
                point("start", "Salida", RoutePointType.START),
                point("end", "Meta", RoutePointType.FINISH)
            ), 10
        )
        assertTrue(RouteValidator().validate(route).issues.isEmpty())
    }
}
