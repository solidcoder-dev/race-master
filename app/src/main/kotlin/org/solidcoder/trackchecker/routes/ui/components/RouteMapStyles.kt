package org.solidcoder.trackchecker.routes.ui.components

internal enum class RouteMapStyle(val label: String, val mapId: String) {
    SATELLITE("Satélite", "hybrid"),
    RELIEF("Relieve", "outdoor-v2")
}

/** MapTiler style URLs are a UI/infrastructure concern, never a route business rule. */
internal object RouteMapStyles {
    const val DEMO_STYLE = "https://demotiles.maplibre.org/style.json"

    fun url(style: RouteMapStyle, apiKey: String): String =
        if (apiKey.isBlank()) DEMO_STYLE
        else "https://api.maptiler.com/maps/${style.mapId}/style.json?key=$apiKey"
}
