package org.solidcoder.trackchecker.routes.ui.components

import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.Style

/** OpenFreeMap vector labels with an open Terrarium elevation overlay. */
internal object RouteMapStyles {
    const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
    const val TERRAIN_URL = "https://tiles.mapterhorn.com/tilejson.json"
    const val ATTRIBUTION = "© OpenFreeMap · © OpenMapTiles · © OpenStreetMap contributors · © Mapterhorn"
    const val FALLBACK_TILE_URL = "https://a.tile.opentopomap.org/{z}/{x}/{y}.png"
    const val FALLBACK_ATTRIBUTION = "© OpenTopoMap · © OpenStreetMap contributors · SRTM"

    fun localizedTerrainStyle(styleJson: String): Style.Builder {
        val style = JSONObject(styleJson)
        val sources = style.getJSONObject("sources")
        sources.put(
            "trackchecker-terrain",
            JSONObject()
                .put("type", "raster-dem")
                .put("url", TERRAIN_URL)
                .put("encoding", "terrarium")
                .put("tileSize", 512)
                .put("attribution", "© Mapterhorn")
        )

        val layers = style.getJSONArray("layers")
        var firstLabelIndex = layers.length()
        for (index in 0 until layers.length()) {
            val layer = layers.getJSONObject(index)
            if (layer.optString("type") != "symbol") continue
            val layout = layer.optJSONObject("layout") ?: continue
            val field = layout.opt("text-field") ?: continue
            if (containsNameField(field)) {
                layout.put("text-field", spanishEnglishFallback())
            }
            firstLabelIndex = minOf(firstLabelIndex, index)
        }

        val hillshade = JSONObject()
            .put("id", "trackchecker-terrain-hillshade")
            .put("type", "hillshade")
            .put("source", "trackchecker-terrain")
            .put(
                "paint",
                JSONObject()
                    .put("hillshade-illumination-direction", 315)
                    .put("hillshade-exaggeration", 0.45)
            )
        layers.put(firstLabelIndex, hillshade)
        return Style.Builder().fromJson(style.toString())
    }

    /** Raster fallback retains visible topographic shading if the vector style cannot be fetched. */
    fun fallbackStyle(): Style.Builder = Style.Builder().fromJson(
        """
        {
          "version": 8,
          "sources": {
            "opentopomap": {
              "type": "raster",
              "tiles": ["$FALLBACK_TILE_URL"],
              "tileSize": 256,
              "attribution": "$FALLBACK_ATTRIBUTION"
            }
          },
          "layers": [{"id": "opentopomap", "type": "raster", "source": "opentopomap"}]
        }
        """.trimIndent()
    )

    private fun spanishEnglishFallback(): JSONArray = JSONArray()
        .put("coalesce")
        .put(JSONArray().put("get").put("name:es"))
        .put(JSONArray().put("get").put("name_es"))
        .put(JSONArray().put("get").put("name_en"))
        .put(JSONArray().put("get").put("name:en"))
        .put(JSONArray().put("get").put("name:latin"))
        .put(JSONArray().put("get").put("name"))

    private fun containsNameField(value: Any): Boolean = when (value) {
        is JSONArray -> (0 until value.length()).any { containsNameField(value.get(it)) }
        is String -> value == "name" || value.startsWith("name:") || value.startsWith("name_")
        else -> false
    }
}
