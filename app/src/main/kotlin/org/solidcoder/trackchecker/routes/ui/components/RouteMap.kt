package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId

/**
 * A native MapLibre map hosted inside Compose. Marker snippets carry stable IDs,
 * because route point names are explicitly allowed to repeat.
 *
 * The MapLibre demo style is for development only. Configure a tile provider
 * before distributing the app for real-world rally use.
 */
@Suppress("DEPRECATION")
@Composable
fun RouteMap(route: Route, onPointClick: (RoutePointId) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentCallback by rememberUpdatedState(onPointClick)
    MapLibre.getInstance(context)

    val mapView = remember(route.id) { MapView(context).apply { onCreate(null) } }
    var controller by remember(route.id) { mutableStateOf<MapLibreMap?>(null) }

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
            map.setStyle("https://demotiles.maplibre.org/style.json") {
                val points = route.points
                if (points.isNotEmpty()) {
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
                    }
                }
                controller = map
            }
        }
        onDispose {
            lifecycle.removeObserver(observer)
            controller = null
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(controller, route.points) {
        controller?.let { map ->
            map.clear()
            route.points.forEach { point ->
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(point.coordinates.latitude, point.coordinates.longitude))
                        .title(point.name)
                        .snippet(point.id.value)
                )
            }
            map.setOnMarkerClickListener { marker ->
                val id = marker.snippet
                if (id != null) currentCallback(RoutePointId(id))
                true
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
