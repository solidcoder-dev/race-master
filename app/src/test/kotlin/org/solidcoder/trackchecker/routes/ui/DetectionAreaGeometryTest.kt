package org.solidcoder.trackchecker.routes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.routes.domain.model.DetectionRadius
import org.solidcoder.trackchecker.routes.ui.components.DetectionAreaGeometry
import kotlin.math.*

class DetectionAreaGeometryTest {
    @Test fun `detection ring has real metre radius even at high latitudes`() {
        val center = Coordinates(60.0, -15.0)
        val radius = DetectionRadius(150.0)
        val ring = DetectionAreaGeometry.ring(center, radius)

        assertEquals(49, ring.size)
        assertEquals(ring.first(), ring.last())
        ring.forEach { assertTrue(distanceMeters(center, it) in 149.9..150.1) }
    }

    private fun distanceMeters(a: Coordinates, b: Coordinates): Double {
        val latitudeDelta = Math.toRadians(b.latitude - a.latitude)
        val longitudeDelta = Math.toRadians(b.longitude - a.longitude)
        val h = sin(latitudeDelta / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
            cos(Math.toRadians(b.latitude)) * sin(longitudeDelta / 2).pow(2)
        return 6_371_008.8 * 2 * atan2(sqrt(h), sqrt(1 - h))
    }
}
