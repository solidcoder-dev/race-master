package org.solidcoder.trackchecker.routes.ui.components

import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.solidcoder.trackchecker.tracks.domain.model.Track

/** One selected recording is drawn as a GeoJSON overlay, never as route annotations. */
internal object RouteMapTrackLayer {
    private const val SOURCE_ID = "selected-gps-track"
    private const val HALO_ID = "selected-gps-track-halo"
    private const val LINE_ID = "selected-gps-track-line"

    fun render(map: MapLibreMap, track: Track?) {
        val style = map.style ?: return
        val lines = track?.segments.orEmpty().mapNotNull { segment ->
            if (segment.samples.size < 2) null else Feature.fromGeometry(LineString.fromLngLats(
                segment.samples.map { Point.fromLngLat(it.longitude, it.latitude) }
            ))
        }
        val geometry = FeatureCollection.fromFeatures(lines)
        val source = style.getSource(SOURCE_ID) as? GeoJsonSource
        if (source != null) {
            source.setGeoJson(geometry)
            return
        }
        style.addSource(GeoJsonSource(SOURCE_ID, geometry))
        style.addLayer(LineLayer(HALO_ID, SOURCE_ID).withProperties(
            lineColor("#FFFFFF"), lineWidth(5f), lineOpacity(0.85f)
        ))
        style.addLayer(LineLayer(LINE_ID, SOURCE_ID).withProperties(
            lineColor("#7951B8"), lineWidth(3f), lineOpacity(0.95f)
        ))
    }
}
