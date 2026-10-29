package org.solidcoder.trackchecker.routes.ui.components

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.solidcoder.trackchecker.routes.domain.model.DetectionRadius
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint

/** Dedicated GeoJSON layers; marker annotations remain interactive and visually above them. */
internal object RouteMapDetectionAreas {
    private const val SOURCE_ID = "route-point-detection-areas"
    private const val FILL_ID = "route-point-detection-fill"
    private const val OUTLINE_ID = "route-point-detection-outline"

    fun render(map: MapLibreMap, points: List<RoutePoint>, defaultRadius: DetectionRadius, visible: Boolean) {
        val style = map.style ?: return
        val polygons = if (visible) points.map { point ->
            val ring = DetectionAreaGeometry.ring(point.coordinates, point.detectionRadiusOverride ?: defaultRadius)
                .map { coordinate -> Point.fromLngLat(coordinate.longitude, coordinate.latitude) }
            Feature.fromGeometry(Polygon.fromLngLats(listOf(ring)))
        } else emptyList()
        val geometry = FeatureCollection.fromFeatures(polygons)

        val source = style.getSource(SOURCE_ID) as? GeoJsonSource
        if (source != null) {
            source.setGeoJson(geometry)
            return
        }
        style.addSource(GeoJsonSource(SOURCE_ID, geometry))
        style.addLayer(FillLayer(FILL_ID, SOURCE_ID).withProperties(
            fillColor("#087F8C"), fillOpacity(0.12f)
        ))
        style.addLayer(LineLayer(OUTLINE_ID, SOURCE_ID).withProperties(
            lineColor("#087F8C"), lineWidth(1.2f), lineOpacity(0.7f)
        ))
    }
}
