package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlin.math.floor
import kotlin.math.pow
import java.net.URL
import java.net.HttpURLConnection
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId
import org.solidcoder.trackchecker.routes.domain.model.DetectionRadius
import kotlin.math.hypot

/**
 * A native MapLibre map hosted inside Compose. Marker snippets carry stable IDs,
 * because route point names are explicitly allowed to repeat.
 *
 * Uses OpenFreeMap vector labels and Mapterhorn elevation tiles. The public
 * OpenTopoMap raster style is retained as a network-error fallback.
 */
@Suppress("DEPRECATION")
@Composable
fun RouteMap(
    routeId: RouteId,
    points: List<RoutePoint>,
    defaultRadius: DetectionRadius,
    showDetectionRadii: Boolean,
    selectedPointId: RoutePointId?,
    selectedPointIds: Set<RoutePointId>,
    onPointClick: (RoutePointId) -> Unit,
    onPointLongPress: (RoutePointId) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentCallback by rememberUpdatedState(onPointClick)
    val currentLongPress by rememberUpdatedState(onPointLongPress)
    MapLibre.getInstance(context)

    val mapView = remember(routeId) { MapView(context).apply { onCreate(null) } }
    val markers = remember(routeId) { RouteMapMarkers(context) }
    var controller by remember(routeId) { mutableStateOf<MapLibreMap?>(null) }
    var styleVersion by remember(routeId) { mutableIntStateOf(0) }
    var initialCameraSet by remember(routeId) { mutableStateOf(false) }
    var scale by remember(routeId) { mutableStateOf(MapScale("1 km", 80f)) }
    var attribution by remember(routeId) { mutableStateOf(RouteMapStyles.FALLBACK_ATTRIBUTION) }

    DisposableEffect(mapView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        mapView.getMapAsync { map ->
            controller = map
        }
        onDispose {
            lifecycle.removeObserver(observer)
            controller = null
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    DisposableEffect(controller, points) {
        val map = controller
        val listener = MapLibreMap.OnMapLongClickListener { location ->
            if (map == null) return@OnMapLongClickListener false
            val touched = map.projection.toScreenLocation(location)
            val thresholdPx = 32f * context.resources.displayMetrics.density
            val nearby = points.map { point ->
                val screen = map.projection.toScreenLocation(
                    LatLng(point.coordinates.latitude, point.coordinates.longitude)
                )
                point.id to hypot((screen.x - touched.x).toDouble(), (screen.y - touched.y).toDouble())
            }.filter { it.second <= thresholdPx }.minByOrNull { it.second }
            if (nearby != null) {
                currentLongPress(nearby.first)
                true
            } else false
        }
        map?.addOnMapLongClickListener(listener)
        onDispose { map?.removeOnMapLongClickListener(listener) }
    }

    LaunchedEffect(controller) {
        controller?.let { map ->
            val localizedStyle = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(RouteMapStyles.STYLE_URL).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 15_000
                    connection.setRequestProperty("User-Agent", "TrackChecker Android")
                    try {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } finally {
                        connection.disconnect()
                    }
                }.getOrNull()
            }
            val vectorStyle = localizedStyle?.let { json ->
                runCatching { RouteMapStyles.localizedTerrainStyle(json) }.getOrNull()
            }
            attribution = if (vectorStyle != null) RouteMapStyles.ATTRIBUTION else RouteMapStyles.FALLBACK_ATTRIBUTION
            val styleBuilder = vectorStyle ?: RouteMapStyles.fallbackStyle()
            map.setStyle(styleBuilder) {
                if (!initialCameraSet && points.isNotEmpty()) {
                    val bounds = LatLngBounds.Builder()
                    points.forEach { point ->
                        bounds.include(LatLng(point.coordinates.latitude, point.coordinates.longitude))
                    }
                    mapView.post {
                        try {
                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 56))
                        } catch (_: IllegalStateException) {
                            val first = points.first()
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(first.coordinates.latitude, first.coordinates.longitude))
                                .zoom(9.0)
                                .build()
                        }
                        initialCameraSet = true
                    }
                }
                updateScale(map, context.resources.displayMetrics.density) { scale = it }
                map.addOnCameraIdleListener {
                    updateScale(map, context.resources.displayMetrics.density) { scale = it }
                }
                styleVersion++
            }
        }
    }

    LaunchedEffect(controller, styleVersion, points) {
        controller?.let { map ->
            markers.show(map, points, selectedPointIds + listOfNotNull(selectedPointId)) { currentCallback(it) }
        }
    }

    LaunchedEffect(controller, styleVersion, selectedPointId, selectedPointIds, points) {
        controller?.let { markers.select(it, selectedPointIds + listOfNotNull(selectedPointId)) }
        val point = points.firstOrNull { it.id == selectedPointId }
        if (point != null) {
            controller?.let { map ->
                map.animateCamera(CameraUpdateFactory.newLatLng(
                    LatLng(point.coordinates.latitude, point.coordinates.longitude)
                ))
            }
        }
    }

    LaunchedEffect(controller, styleVersion, points, defaultRadius, showDetectionRadii) {
        controller?.let { RouteMapDetectionAreas.render(it, points, defaultRadius, showDetectionRadii) }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Surface(
            Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 12.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.9f)
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(scale.label, color = Color(0xFF222222), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                androidx.compose.foundation.layout.Box(
                    Modifier.padding(top = 3.dp).width(scale.widthDp.dp).height(3.dp)
                        .background(Color(0xFF222222), RoundedCornerShape(1.dp))
                )
            }
        }
        Surface(
            Modifier.align(Alignment.BottomEnd).padding(8.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.White.copy(alpha = 0.85f)
        ) {
            Text(
                attribution,
                Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                color = Color(0xFF222222),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
        }
    }
}

private data class MapScale(val label: String, val widthDp: Float)

private fun updateScale(map: MapLibreMap, density: Float, update: (MapScale) -> Unit) {
    val latitude = map.cameraPosition.target?.latitude ?: return
    val metersPerPixel = map.projection.getMetersPerPixelAtLatitude(latitude)
    if (!metersPerPixel.isFinite() || metersPerPixel <= 0.0) return
    val maxMeters = metersPerPixel * 100.0 * density
    val magnitude = 10.0.pow(floor(kotlin.math.log10(maxMeters)))
    val niceDistance = listOf(5.0, 2.0, 1.0).first { it * magnitude <= maxMeters } * magnitude
    update(MapScale(formatDistance(niceDistance), (niceDistance / metersPerPixel / density).toFloat()))
}

private fun formatDistance(meters: Double): String =
    when {
        meters >= 1000.0 -> "${(meters / 1000.0).toInt()} km"
        meters < 10.0 -> "${"%.1f".format(java.util.Locale.ROOT, meters)} m"
        else -> "${meters.toInt()} m"
    }
