package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId
import org.solidcoder.trackchecker.BuildConfig

/**
 * A native MapLibre map hosted inside Compose. Marker snippets carry stable IDs,
 * because route point names are explicitly allowed to repeat.
 *
 * MapTiler provides satellite and shaded outdoor styles when configured.
 * The MapLibre demo style remains an explicit development fallback.
 */
@Suppress("DEPRECATION")
@Composable
fun RouteMap(
    routeId: RouteId,
    points: List<RoutePoint>,
    selectedPointId: RoutePointId?,
    onPointClick: (RoutePointId) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentCallback by rememberUpdatedState(onPointClick)
    val providerKey = BuildConfig.MAPTILER_API_KEY
    var baseMap by remember(routeId) { mutableStateOf(RouteMapStyle.SATELLITE) }
    MapLibre.getInstance(context)

    val mapView = remember(routeId) { MapView(context).apply { onCreate(null) } }
    val markers = remember(routeId) { RouteMapMarkers(context) }
    var controller by remember(routeId) { mutableStateOf<MapLibreMap?>(null) }
    var styleVersion by remember(routeId) { mutableIntStateOf(0) }
    var initialCameraSet by remember(routeId) { mutableStateOf(false) }

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

    LaunchedEffect(controller, baseMap) {
        controller?.let { map ->
            map.setStyle(RouteMapStyles.url(baseMap, providerKey)) {
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
                styleVersion++
            }
        }
    }

    LaunchedEffect(controller, styleVersion, points) {
        controller?.let { map ->
            markers.show(map, points, selectedPointId) { currentCallback(it) }
        }
    }

    LaunchedEffect(controller, selectedPointId, points) {
        val point = points.firstOrNull { it.id == selectedPointId }
        if (point != null) {
            controller?.let { map ->
                markers.select(map, selectedPointId)
                map.animateCamera(CameraUpdateFactory.newLatLng(
                    LatLng(point.coordinates.latitude, point.coordinates.longitude)
                ))
            }
        } else {
            controller?.let { markers.select(it, null) }
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        if (providerKey.isNotBlank()) {
            Surface(Modifier.align(Alignment.TopEnd).padding(12.dp), shape = androidx.compose.material3.MaterialTheme.shapes.large) {
                Row(Modifier.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RouteMapStyle.entries.forEach { style ->
                        FilterChip(
                            selected = baseMap == style,
                            onClick = { baseMap = style },
                            label = { androidx.compose.material3.Text(style.label) }
                        )
                    }
                }
            }
        }
    }
}
