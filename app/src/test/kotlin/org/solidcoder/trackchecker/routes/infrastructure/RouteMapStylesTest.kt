package org.solidcoder.trackchecker.routes.infrastructure

import org.junit.Assert.assertEquals
import org.junit.Test
import org.solidcoder.trackchecker.routes.ui.components.RouteMapStyle
import org.solidcoder.trackchecker.routes.ui.components.RouteMapStyles

class RouteMapStylesTest {
    @Test fun `satellite and relief resolve to distinct configured map styles`() {
        assertEquals("https://api.maptiler.com/maps/hybrid/style.json?key=demo",
            RouteMapStyles.url(RouteMapStyle.SATELLITE, "demo"))
        assertEquals("https://api.maptiler.com/maps/outdoor-v2/style.json?key=demo",
            RouteMapStyles.url(RouteMapStyle.RELIEF, "demo"))
    }

    @Test fun `map remains usable in development without a provider key`() {
        assertEquals(RouteMapStyles.DEMO_STYLE, RouteMapStyles.url(RouteMapStyle.SATELLITE, ""))
    }
}
