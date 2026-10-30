package org.solidcoder.trackchecker.routes.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.solidcoder.trackchecker.routes.application.usecases.RouteUseCases
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource
import org.solidcoder.trackchecker.scoring.application.usecases.TrackCheckActions
import org.solidcoder.trackchecker.tracks.application.ports.TrackImportSource
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.ui.screens.RoutesListScreen
import org.solidcoder.trackchecker.routes.ui.screens.RouteReviewScreen
import org.solidcoder.trackchecker.tracks.ui.screens.TracksListScreen

@Composable
fun RoutesApp(
    actions: RouteUseCases,
    trackChecks: TrackCheckActions,
    readSelectedRoute: suspend (Uri) -> RouteImportSource,
    readSelectedTrack: suspend (Uri) -> TrackImportSource
) {
    val viewModel: RoutesViewModel = viewModel(factory = remember(actions, trackChecks) {
        RoutesViewModel.factory(actions, trackChecks)
    })
    val routesState by viewModel.routes.collectAsStateWithLifecycle()
    val reviewState by viewModel.review.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var navigation by rememberSaveable(stateSaver = TrackNavigationState.Saver) {
        mutableStateOf(TrackNavigationState(TrackDestination.ROUTES))
    }
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            viewModel.cancelImport()
        } else scope.launch {
            try {
                val source = readSelectedRoute(uri)
                viewModel.import(source) { navigation = navigation.openRoute(it.value) }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                viewModel.report(error)
            }
        }
    }

    val trackFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val routeId = navigation.routeId?.let(::RouteId)
        if (uri != null && routeId != null) scope.launch {
            try {
                viewModel.importTrack(routeId, readSelectedTrack(uri))
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                viewModel.report(error)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RoutesUiEvent.Message -> snackbar.showSnackbar(
                    message = event.text,
                    duration = if (event.isError) androidx.compose.material3.SnackbarDuration.Long
                    else androidx.compose.material3.SnackbarDuration.Short
                )
            }
        }
    }
    LaunchedEffect(navigation.routeId) {
        navigation.routeId?.let { viewModel.open(RouteId(it)) }
    }

    BackHandler(enabled = navigation.destination != TrackDestination.ROUTES) {
        navigation = navigation.back()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        if (navigation.destination == TrackDestination.ROUTES) {
            RoutesListScreen(
                state = routesState,
                modifier = Modifier.padding(insets),
                onImport = {
                    viewModel.beginImport()
                    try {
                        filePicker.launch(arrayOf("*/*"))
                    } catch (error: Exception) {
                        viewModel.report(error)
                    }
                },
                onOpen = { navigation = navigation.openRoute(it.value) },
                onRename = { id, name -> viewModel.rename(id, name) },
                onDelete = { id -> viewModel.delete(id) {} }
            )
        } else if (navigation.destination == TrackDestination.ROUTE_REVIEW) {
            val routeId = requireNotNull(navigation.routeId).let(::RouteId)
            RouteReviewScreen(
                state = reviewState,
                modifier = Modifier.padding(insets),
                onBack = { navigation = navigation.back() },
                onEdit = { pointId, name, type, changes ->
                    viewModel.updatePoint(routeId, pointId, name, type, changes)
                },
                onEditSelected = { pointIds, changes ->
                    viewModel.updatePoints(routeId, pointIds, changes)
                },
                onSetRadius = { radius -> viewModel.setDetectionRadius(routeId, radius) },
                onApplyPattern = { pattern, replace ->
                    viewModel.applyPattern(routeId, pattern, replace)
                },
                onMarkReady = { viewModel.markReady(routeId) },
                onOpenTracks = { navigation = navigation.openTracks() }
            )
        } else if (navigation.destination == TrackDestination.TRACKS) {
            val loaded = reviewState as? RouteReviewUiState.Loaded
            if (loaded == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                TracksListScreen(
                    route = loaded.route,
                    tracks = loaded.tracks,
                    onBack = { navigation = navigation.back() },
                    onImport = { trackFilePicker.launch(arrayOf("*/*")) },
                    onOpen = { navigation = navigation.openTrackDetail(it.value) }
                )
            }
        } else {
            TrackNavigationPlaceholder(navigation.destination, onBack = { navigation = navigation.back() })
        }
    }
}
