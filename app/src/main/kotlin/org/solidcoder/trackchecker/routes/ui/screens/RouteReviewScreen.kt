package org.solidcoder.trackchecker.routes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.ui.RouteReviewUiState
import org.solidcoder.trackchecker.routes.ui.components.PointEditorSheet
import org.solidcoder.trackchecker.routes.ui.components.RouteIssues
import org.solidcoder.trackchecker.routes.ui.components.RouteMap
import org.solidcoder.trackchecker.routes.ui.components.PointTypeDot
import org.solidcoder.trackchecker.routes.ui.components.PointNameSuggestions
import org.solidcoder.trackchecker.routes.ui.components.displayName
import org.solidcoder.trackchecker.routes.ui.components.displayMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteReviewScreen(
    state: RouteReviewUiState,
    onBack: () -> Unit,
    onEdit: (RoutePointId, String, RoutePointType) -> Unit,
    onMarkReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loaded = when (state) {
        RouteReviewUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        RouteReviewUiState.NotFound -> {
            RouteReviewUnavailable("Esta ruta ya no existe", onBack, modifier)
            return
        }
        is RouteReviewUiState.Failed -> {
            RouteReviewUnavailable(state.message, onBack, modifier)
            return
        }
        is RouteReviewUiState.Loaded -> state
    }
    val route = loaded.route

    var editingPointId by remember(route.id) { mutableStateOf<RoutePointId?>(null) }
    val selectedPoint = route.points.firstOrNull { it.id == editingPointId }
    val sheetScaffold = rememberBottomSheetScaffoldState()
    val density = LocalDensity.current
    var summaryHeightPx by remember(route.id) { mutableIntStateOf(0) }
    val peekHeight = if (summaryHeightPx == 0) 180.dp else
        with(density) { summaryHeightPx.toDp() } + 48.dp

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = sheetScaffold,
        sheetPeekHeight = peekHeight,
        topBar = {
            TopAppBar(
                title = { Text(route.name, maxLines = 1) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        },
        sheetContent = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.86f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth().onSizeChanged { summaryHeightPx = it.height },
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "${route.points.size} puntos · ${loaded.issues.size} incidencias",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        loaded.issues.firstOrNull()?.let { issue ->
                            Text(
                                issue.displayMessage(route),
                                color = if (issue.severity == IssueSeverity.ERROR)
                                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onMarkReady,
                            enabled = route.status != RouteStatus.READY && loaded.issues.none { it.severity == IssueSeverity.ERROR },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (route.status == RouteStatus.READY) "Ruta lista" else "Validar ruta")
                        }
                        Text(
                            "Desliza hacia arriba para ver incidencias y puntos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (loaded.issues.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            RouteIssues(route = route, issues = loaded.issues, onSelectPoint = { editingPointId = it })
                        }
                    }
                }
                items(route.points, key = { it.id.value }) { point ->
                    Row(
                        Modifier.fillMaxWidth().clickable { editingPointId = point.id }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(point.name, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PointTypeDot(point.type)
                                Text(point.type.displayName(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    ) { contentPadding ->
        RouteMap(
            routeId = route.id,
            points = route.points,
            selectedPointId = editingPointId,
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            onPointClick = { editingPointId = it }
        )
    }

    selectedPoint?.let { point ->
        PointEditorSheet(
            point = point,
            suggestedName = PointNameSuggestions.forPoint(route.points, point.id),
            onDismiss = { editingPointId = null },
            onSave = { name, type ->
                onEdit(point.id, name, type)
                editingPointId = null
            }
        )
    }
}

@Composable
private fun RouteReviewUnavailable(message: String, onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Volver a mis rutas") }
    }
}
