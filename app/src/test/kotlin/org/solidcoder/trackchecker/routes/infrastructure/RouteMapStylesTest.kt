package org.solidcoder.trackchecker.routes.infrastructure

import org.junit.Assert.assertTrue
import org.junit.Test
import org.solidcoder.trackchecker.routes.ui.components.RouteMapStyles
import org.json.JSONObject

class RouteMapStylesTest {
    @Test fun `vector style prioritizes Spanish and English labels and adds terrain`() {
        val baseStyle = """
            {"version":8,"sources":{"openmaptiles":{"type":"vector","url":"https://example.org/tiles.json"}},
             "layers":[{"id":"place_names","type":"symbol","source":"openmaptiles","source-layer":"place",
               "layout":{"text-field":["coalesce",["get","name:latin"],["get","name"]]}}]}
        """.trimIndent()
        val result = JSONObject(RouteMapStyles.localizedTerrainStyle(baseStyle).json)
        val terrain = result.getJSONObject("sources").getJSONObject("trackchecker-terrain")
        val labels = result.getJSONArray("layers").getJSONObject(1).getJSONObject("layout").getJSONArray("text-field")

        assertTrue(terrain.getString("url").contains("mapterhorn.com"))
        assertTrue(terrain.getString("encoding") == "terrarium")
        assertTrue(labels.toString().contains("name:es"))
        assertTrue(labels.toString().contains("name_en"))
        assertTrue(RouteMapStyles.ATTRIBUTION.contains("OpenFreeMap"))
        assertTrue(RouteMapStyles.ATTRIBUTION.contains("OpenStreetMap"))
    }
}
