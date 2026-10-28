package org.solidcoder.trackchecker.routes.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.solidcoder.trackchecker.routes.application.usecases.RouteUseCases
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.ui.screens.RoutesListScreen
import org.solidcoder.trackchecker.routes.ui.screens.RouteReviewScreen

@Composable
fun RoutesApp(actions: RouteUseCases) {
    val viewModel: RoutesViewModel = viewModel(factory = remember(actions) { RoutesViewModel.factory(actions) })
    val routesState by viewModel.routes.collectAsStateWithLifecycle()
    val reviewState by viewModel.review.collectAsStateWithLifecycle()
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val (fileName, bytes) = readSelectedRoute(context.contentResolver, uri)
                viewModel.import(fileName, bytes) { selectedRouteId = it.value }
            } catch (error: Exception) {
                viewModel.report(error)
            }
        }
    }

    LaunchedEffect(routesState.error) {
        routesState.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
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
                onImport = { filePicker.launch(arrayOf("*/*")) },
                onOpen = { selectedRouteId = it.value },
                onRename = { id, name -> viewModel.rename(id, name) },
                onDelete = { id -> viewModel.delete(id) {} }
            )
        } else {
            RouteReviewScreen(
                state = reviewState,
                modifier = Modifier.padding(insets),
                onBack = { selectedRouteId = null },
                onEdit = { pointId, name, type ->
                    viewModel.updatePoint(RouteId(selectedRouteId!!), pointId, name, type)
                },
                onValidate = { viewModel.validate(RouteId(selectedRouteId!!)) }
            )
        }
    }
}
