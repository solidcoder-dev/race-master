package org.solidcoder.trackchecker.routes.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.ui.RouteReviewUiState
import org.solidcoder.trackchecker.routes.ui.components.*
import org.solidcoder.trackchecker.scoring.ui.components.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RouteReviewScreen(
    state: RouteReviewUiState,
    onBack: () -> Unit,
    onEdit: (RoutePointId, String, RoutePointType, PointChanges?) -> Unit,
    onEditSelected: (Set<RoutePointId>, PointChanges) -> Unit,
    onSetRadius: (DetectionRadius) -> Unit,
    onApplyPattern: (ScoringPattern, Boolean) -> Unit,
    onMarkReady: () -> Unit,
    onOpenTracks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loaded = when (state) {
        RouteReviewUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
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
    val routeReady = route.status == RouteStatus.READY && loaded.issues.none { it.severity == IssueSeverity.ERROR }
    var editingPointId by remember(route.id) { mutableStateOf<RoutePointId?>(null) }
    var selectedIds by remember(route.id) { mutableStateOf<Set<RoutePointId>>(emptySet()) }
    var selectedExpanded by remember(route.id) { mutableStateOf(false) }
    var editingSelected by remember(route.id) { mutableStateOf(false) }
    var settingsOpen by remember(route.id) { mutableStateOf(false) }
    var patternOpen by remember(route.id) { mutableStateOf(false) }
    var showRadii by remember(route.id) { mutableStateOf(false) }
    val selectionActive = selectedIds.isNotEmpty()
    val selectedPoint = route.points.firstOrNull { it.id == editingPointId }

    fun toggleSelection(id: RoutePointId) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        if (selectedIds.isEmpty()) selectedExpanded = false
        editingPointId = null
    }
    fun openPoint(id: RoutePointId) {
        if (selectedIds.isNotEmpty()) toggleSelection(id) else editingPointId = id
    }

    val sheetScaffold = rememberBottomSheetScaffoldState()
    val density = LocalDensity.current
    var summaryHeightPx by remember(route.id) { mutableIntStateOf(0) }
    val peekHeight = if (summaryHeightPx == 0) 180.dp
        else with(density) { summaryHeightPx.toDp() } + 48.dp

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = sheetScaffold,
        sheetPeekHeight = peekHeight,
        topBar = {
            RouteReviewHeader(
                routeName = route.name,
                selectionCount = selectedIds.size,
                showDetectionRadii = showRadii,
                onNavigate = { if (selectionActive) selectedIds = emptySet() else onBack() },
                onEditSelected = { editingSelected = true },
                onToggleRadii = { showRadii = !showRadii },
                onOpenSettings = { settingsOpen = true },
                onOpenPatterns = { patternOpen = true }
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
                        if (selectionActive) {
                            Text("${selectedIds.size} puntos seleccionados",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { selectedExpanded = !selectedExpanded }) {
                                Text(if (selectedExpanded) "Ocultar selección" else "Ver selección (${selectedIds.size})")
                            }
                        } else {
                            RouteTracksEntry(
                                trackCount = loaded.tracks.size,
                                routeReady = routeReady,
                                canValidate = loaded.issues.none { it.severity == IssueSeverity.ERROR },
                                onOpen = onOpenTracks,
                                onValidate = onMarkReady
                            )
                            Text("${route.points.size} puntos · ${loaded.issues.size} incidencias",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            loaded.issues.firstOrNull()?.let { issue ->
                                Text(issue.displayMessage(route),
                                    color = if (issue.severity == IssueSeverity.ERROR)
                                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("Desliza hacia arriba para ver incidencias y puntos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (selectionActive && selectedExpanded) {
                    item {
                        Column {
                            route.points.filter { it.id in selectedIds }.forEach { point ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    PointTypeDot(point.type)
                                    Spacer(Modifier.width(8.dp))
                                    Text(point.name, modifier = Modifier.weight(1f), maxLines = 1)
                                    TextButton(onClick = { toggleSelection(point.id) }) { Text("Quitar") }
                                }
                            }
                        }
                    }
                }
                if (!selectionActive && loaded.issues.isNotEmpty()) {
                    item { RouteIssues(route = route, issues = loaded.issues, onSelectPoint = { openPoint(it) }) }
                }
                items(route.points, key = { it.id.value }) { point ->
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(
                            onClick = { openPoint(point.id) },
                            onLongClick = { toggleSelection(point.id) }
                        ).padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionActive) {
                            Checkbox(checked = point.id in selectedIds, onCheckedChange = { toggleSelection(point.id) })
                        }
                        Column(Modifier.weight(1f)) {
                            Text(point.name, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PointTypeDot(point.type)
                                Text(point.type.displayName(), style = MaterialTheme.typography.bodySmall)
                                val score = route.scoringFor(point.id)
                                val scoreText = when (point.type) {
                                    RoutePointType.WAYPOINT -> score?.whenPassed?.let { "+$it pts" }
                                    RoutePointType.CHECKPOINT -> score?.whenMissed?.let { "$it pts" }
                                    else -> null
                                }
                                scoreText?.let { Text("· $it", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                        if (!selectionActive) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    ) { contentPadding ->
        RouteMap(
            routeId = route.id,
            points = route.points,
            defaultRadius = route.defaultDetectionRadius,
            showDetectionRadii = showRadii,
            selectedPointId = editingPointId,
            selectedPointIds = selectedIds,
            track = null,
            passageStatuses = emptyMap(),
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            onPointClick = { openPoint(it) },
            onPointLongPress = { toggleSelection(it) }
        )
    }

    selectedPoint?.let { point ->
        PointEditorSheet(point = point, defaultRadius = route.defaultDetectionRadius,
            defaultScore = when (point.type) {
                RoutePointType.WAYPOINT -> route.scoringFor(point.id)?.whenPassed
                RoutePointType.CHECKPOINT -> route.scoringFor(point.id)?.whenMissed?.let { -it }
                else -> null
            },
            suggestedName = PointNameSuggestions.forPoint(route.points, point.id),
            onDismiss = { editingPointId = null },
            onSave = { name, type, changes ->
                onEdit(point.id, name, type, changes)
                editingPointId = null
            })
    }
    if (editingSelected && selectionActive) {
        BulkPointEditorSheet(count = selectedIds.size, onDismiss = { editingSelected = false }, onSave = { changes ->
            onEditSelected(selectedIds, changes)
            selectedIds = emptySet()
            selectedExpanded = false
            editingSelected = false
        })
    }
    if (settingsOpen) RouteSettingsSheet(route.defaultDetectionRadius,
        onDismiss = { settingsOpen = false }, onSave = {
            onSetRadius(it)
            settingsOpen = false
        })
    if (patternOpen) ScoringPatternDialog(
        current = route.scoringPattern,
        overrideCount = route.points.count { it.scoreValueOverride != null },
        onDismiss = { patternOpen = false },
        onApply = { pattern, replace ->
            onApplyPattern(pattern, replace)
            patternOpen = false
        }
    )
}

@Composable
private fun RouteReviewUnavailable(message: String, onBack: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Volver a mis rutas") }
    }
}
