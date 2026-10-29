@file:Suppress("DEPRECATION")

package org.solidcoder.trackchecker.routes.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus

/**
 * Manages the map's point annotations. A selection changes at most two icons;
 * new bitmap assets and all annotations are created only when points or style change.
 */
@Suppress("DEPRECATION")
internal class RouteMapMarkers(context: Context) {
    private val iconFactory = IconFactory.getInstance(context)
    private val density = context.resources.displayMetrics.density
    private val statuses = listOf<PassageStatus?>(null) + PassageStatus.entries
    private val icons = RoutePointType.entries.flatMap { type -> statuses.map { status -> type to status } }
        .associateWith { (type, status) -> iconFactory.fromBitmap(pointMarkerBitmap(type, density, status = status)) }
    private val selectedIcons = RoutePointType.entries.flatMap { type -> statuses.map { status -> type to status } }
        .associateWith { (type, status) -> iconFactory.fromBitmap(pointMarkerBitmap(type, density, selected = true, status = status)) }
    private var markers: Map<RoutePointId, Marker> = emptyMap()
    private var selectedPointIds: Set<RoutePointId> = emptySet()

    fun show(map: MapLibreMap, points: List<RoutePoint>, selection: Set<RoutePointId>,
             passages: Map<RoutePointId, PassageStatus>, onPointClick: (RoutePointId) -> Unit) {
        // MapLibre's clear() also removes non-marker overlays owned by other components.
        markers.values.forEach { map.removeMarker(it) }
        selectedPointIds = selection
        markerTypes = points.associate { it.id to it.type }
        markerPassages = passages
        markers = points.associate { point ->
            point.id to map.addMarker(
                MarkerOptions()
                    .position(LatLng(point.coordinates.latitude, point.coordinates.longitude))
                    .title(point.name)
                    .snippet(point.id.value)
                    .icon(iconFor(point.type, selected = point.id in selection, status = passages[point.id]))
            )
        }
        map.setOnMarkerClickListener { marker ->
            marker.snippet?.let { onPointClick(RoutePointId(it)) }
            true
        }
    }

    fun select(map: MapLibreMap, selection: Set<RoutePointId>) {
        if (selectedPointIds == selection) return
        val changed = (selectedPointIds - selection) + (selection - selectedPointIds)
        selectedPointIds = selection
        changed.forEach { id ->
            val marker = markers[id] ?: return@forEach
            val type = markerTypes[id] ?: return@forEach
            marker.icon = iconFor(type, selected = id in selection, status = markerPassages[id])
            map.updateMarker(marker)
        }
    }

    private var markerTypes: Map<RoutePointId, RoutePointType> = emptyMap()
    private var markerPassages: Map<RoutePointId, PassageStatus> = emptyMap()

    private fun iconFor(type: RoutePointType, selected: Boolean, status: PassageStatus?): Icon =
        if (selected) selectedIcons.getValue(type to status) else icons.getValue(type to status)

    private fun pointMarkerBitmap(type: RoutePointType, density: Float, selected: Boolean = false,
                                  status: PassageStatus? = null): Bitmap {
        val size = (30 * density).toInt().coerceAtLeast(30)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = size / 2f
        paint.color = if (selected) 0xFFFFD54F.toInt() else android.graphics.Color.WHITE
        canvas.drawCircle(center, center, size * 0.47f, paint)
        paint.color = PointVisuals.color(type)
        canvas.drawCircle(center, center, size * 0.39f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = size * 0.42f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(PointVisuals.symbol(type), center, center - (paint.ascent() + paint.descent()) / 2f, paint)
        if (status != null) {
            val x = size * 0.78f
            val y = size * 0.79f
            paint.color = android.graphics.Color.WHITE
            canvas.drawCircle(x, y, size * 0.19f, paint)
            paint.color = when (status) {
                PassageStatus.PASSED -> 0xFF2E7D32.toInt()
                PassageStatus.MISSED -> 0xFFC62828.toInt()
                PassageStatus.UNCERTAIN -> 0xFFFFA000.toInt()
            }
            canvas.drawCircle(x, y, size * 0.15f, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = size * 0.22f
            canvas.drawText(if (status == PassageStatus.PASSED) "✓" else "!", x,
                y - (paint.ascent() + paint.descent()) / 2f, paint)
        }
        return bitmap
    }
}
