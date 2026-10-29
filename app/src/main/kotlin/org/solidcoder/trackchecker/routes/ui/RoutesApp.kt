package org.solidcoder.trackchecker.routes.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.solidcoder.trackchecker.routes.application.usecases.RouteUseCases
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.ui.screens.RoutesListScreen
import org.solidcoder.trackchecker.routes.ui.screens.RouteReviewScreen

@Composable
fun RoutesApp(
    actions: RouteUseCases,
    readSelectedRoute: suspend (Uri) -> RouteImportSource
) {
    val viewModel: RoutesViewModel = viewModel(factory = remember(actions) { RoutesViewModel.factory(actions) })
    val routesState by viewModel.routes.collectAsStateWithLifecycle()
    val reviewState by viewModel.review.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            viewModel.cancelImport()
        } else scope.launch {
            try {
                val source = readSelectedRoute(uri)
                viewModel.import(source) { selectedRouteId = it.value }
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
    LaunchedEffect(selectedRouteId) {
        selectedRouteId?.let { viewModel.open(RouteId(it)) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        if (selectedRouteId == null) {
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
                onOpen = { selectedRouteId = it.value },
                onRename = { id, name -> viewModel.rename(id, name) },
                onDelete = { id -> viewModel.delete(id) {} }
            )
        } else {
            RouteReviewScreen(
                state = reviewState,
                modifier = Modifier.padding(insets),
                onBack = { selectedRouteId = null },
                onEdit = { pointId, name, type, changes ->
                    viewModel.updatePoint(RouteId(selectedRouteId!!), pointId, name, type, changes)
                },
                onEditSelected = { pointIds, changes ->
                    viewModel.updatePoints(RouteId(selectedRouteId!!), pointIds, changes)
                },
                onSetRadius = { radius -> viewModel.setDetectionRadius(RouteId(selectedRouteId!!), radius) },
                onApplyPattern = { pattern, replace ->
                    viewModel.applyPattern(RouteId(selectedRouteId!!), pattern, replace)
                },
                onMarkReady = { viewModel.markReady(RouteId(selectedRouteId!!)) }
            )
        }
    }
}
