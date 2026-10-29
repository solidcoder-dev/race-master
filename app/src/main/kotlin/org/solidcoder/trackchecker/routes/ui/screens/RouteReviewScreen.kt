package org.solidcoder.trackchecker.routes.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import java.util.Locale
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.ui.RouteReviewUiState
import org.solidcoder.trackchecker.routes.ui.components.*
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus
import org.solidcoder.trackchecker.tracks.domain.model.TrackId

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
    onImportTrack: () -> Unit,
    onSelectTrack: (TrackId?) -> Unit,
    onRecalculateTrack: (TrackId) -> Unit,
    onDeleteTrack: (TrackId) -> Unit,
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
    val activeTrack = loaded.tracks.firstOrNull { it.track.id == loaded.selectedTrackId }
    val routeReady = route.status == RouteStatus.READY && loaded.issues.none { it.severity == IssueSeverity.ERROR }
    val passageById = activeTrack?.evaluation?.passageReport?.passages?.associateBy { it.pointId }.orEmpty()
    var editingPointId by remember(route.id) { mutableStateOf<RoutePointId?>(null) }
    var inspectedPointId by remember(route.id) { mutableStateOf<RoutePointId?>(null) }
    var trackSelectorExpanded by remember(route.id) { mutableStateOf(false) }
    var trackToDelete by remember(route.id) { mutableStateOf<TrackId?>(null) }
    var selectedIds by remember(route.id) { mutableStateOf<Set<RoutePointId>>(emptySet()) }
    var selectedExpanded by remember(route.id) { mutableStateOf(false) }
    var editingSelected by remember(route.id) { mutableStateOf(false) }
    var settingsOpen by remember(route.id) { mutableStateOf(false) }
    var patternOpen by remember(route.id) { mutableStateOf(false) }
    var showRadii by remember(route.id) { mutableStateOf(false) }
    val selectionActive = activeTrack == null && selectedIds.isNotEmpty()
    val selectedPoint = if (activeTrack == null) route.points.firstOrNull { it.id == editingPointId } else null

    LaunchedEffect(activeTrack?.track?.id) {
        inspectedPointId = null
        if (activeTrack != null) {
            selectedIds = emptySet()
            editingPointId = null
            editingSelected = false
        }
    }

    fun toggleSelection(id: RoutePointId) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        if (selectedIds.isEmpty()) selectedExpanded = false
        editingPointId = null
    }
    fun openPoint(id: RoutePointId) {
        if (activeTrack != null) inspectedPointId = id
        else if (selectedIds.isNotEmpty()) toggleSelection(id) else editingPointId = id
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
                selectionCount = if (activeTrack == null) selectedIds.size else 0,
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
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Box {
                                    TextButton(onClick = { trackSelectorExpanded = true }) {
                                        Text(activeTrack?.track?.name ?: "Ruta oficial", maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Text(" ⌄")
                                    }
                                    DropdownMenu(expanded = trackSelectorExpanded,
                                        onDismissRequest = { trackSelectorExpanded = false }) {
                                        DropdownMenuItem(text = { Text("Ruta oficial") }, onClick = {
                                            trackSelectorExpanded = false
                                            onSelectTrack(null)
                                        })
                                        loaded.tracks.forEach { item ->
                                            DropdownMenuItem(text = {
                                                Text("${item.track.name} · ${item.evaluation.score?.total?.let { "$it pts" } ?: "Sin puntuación"}")
                                            }, onClick = {
                                                trackSelectorExpanded = false
                                                onSelectTrack(item.track.id)
                                            })
                                        }
                                    }
                                }
                                Text(if (activeTrack != null) "Evaluado" else if (routeReady) "Lista" else "Borrador",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            if (activeTrack == null) {
                                Text("${route.points.size} puntos · ${loaded.issues.size} incidencias",
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                loaded.issues.firstOrNull()?.let { issue ->
                                    Text(issue.displayMessage(route),
                                        color = if (issue.severity == IssueSeverity.ERROR)
                                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            } else {
                                val evaluation = activeTrack.evaluation
                                val reached = evaluation.passageReport.passages.count { it.status == PassageStatus.PASSED }
                                val missed = evaluation.passageReport.passages.count { it.status == PassageStatus.MISSED }
                                Text("${evaluation.score?.total?.let { "$it pts" } ?: "Sin puntuación"} · $reached alcanzados · $missed no alcanzados",
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (evaluation.isOutdatedFor(route)) {
                                    Text("Resultado desactualizado: han cambiado las reglas de la ruta",
                                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    if (routeReady) TextButton(onClick = { onRecalculateTrack(activeTrack.track.id) }) {
                                        Text("Recalcular")
                                    }
                                } else if (evaluation.passageReport.needsReview) {
                                    Text("Requiere revisión de salida, meta o muestras GPS",
                                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Button(onClick = if (routeReady) onImportTrack else onMarkReady,
                                enabled = if (routeReady) !loaded.importingTrack else
                                    loaded.issues.none { it.severity == IssueSeverity.ERROR },
                                modifier = Modifier.fillMaxWidth()) {
                                Text(if (loaded.importingTrack) "Evaluando…" else if (routeReady)
                                    "Importar track" else "Validar ruta")
                            }
                            Text(if (activeTrack == null) "Desliza hacia arriba para ver incidencias y puntos"
                                 else "Desliza hacia arriba para ver el resultado de cada punto",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (activeTrack != null) {
                    val contributions = activeTrack.evaluation.score?.contributions
                        ?.associate { it.pointId to it.points }.orEmpty()
                    items(route.points, key = { it.id.value }) { point ->
                        val passage = passageById[point.id]
                        Row(Modifier.fillMaxWidth().clickable { inspectedPointId = point.id }
                            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            PointTypeDot(point.type)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(point.name, fontWeight = FontWeight.Medium)
                                Text(when (passage?.status) {
                                    PassageStatus.PASSED -> "Alcanzado"
                                    PassageStatus.MISSED -> "No alcanzado"
                                    PassageStatus.UNCERTAIN -> "Paso indeterminado"
                                    null -> "Sin evaluación"
                                }, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            contributions[point.id]?.let { value ->
                                Text(if (value > 0) "+$value pts" else "$value pts",
                                    style = MaterialTheme.typography.labelMedium)
                            }
                            Text(" ›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider()
                    }
                    item {
                        TextButton(onClick = { trackToDelete = activeTrack.track.id },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Text("Eliminar track")
                        }
                    }
                } else {
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
        }
    ) { contentPadding ->
        RouteMap(
            routeId = route.id,
            points = route.points,
            defaultRadius = route.defaultDetectionRadius,
            showDetectionRadii = showRadii,
            selectedPointId = if (activeTrack == null) editingPointId else inspectedPointId,
            selectedPointIds = if (activeTrack == null) selectedIds else emptySet(),
            track = activeTrack?.track,
            passageStatuses = passageById.mapValues { it.value.status },
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            onPointClick = { openPoint(it) },
            onPointLongPress = { if (activeTrack == null) toggleSelection(it) }
        )
    }

    inspectedPointId?.let { pointId ->
        if (activeTrack != null) {
            val point = route.points.firstOrNull { it.id == pointId }
            val passage = passageById[pointId]
            if (point != null && passage != null) {
                val contribution = activeTrack.evaluation.score?.contributions?.firstOrNull { it.pointId == pointId }
                AlertDialog(
                    onDismissRequest = { inspectedPointId = null },
                    title = { Text(point.name) },
                    text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Estado: " + when (passage.status) {
                            PassageStatus.PASSED -> "Alcanzado"
                            PassageStatus.MISSED -> "No alcanzado"
                            PassageStatus.UNCERTAIN -> "Indeterminado"
                        })
                        Text("Distancia mínima: ${String.format(Locale.ROOT, "%.0f", passage.minimumDistanceMeters)} m")
                        Text("Radio de detección: ${String.format(Locale.ROOT, "%.0f", passage.detectionRadiusMeters)} m")
                        contribution?.let { Text("Puntuación: ${it.points} pts") }
                    } },
                    confirmButton = { TextButton(onClick = { inspectedPointId = null }) { Text("Cerrar") } }
                )
            }
        }
    }

    trackToDelete?.let { trackId ->
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text("¿Eliminar este track?") },
            text = { Text("Se eliminarán el recorrido importado y su evaluación. La ruta oficial no cambiará.") },
            confirmButton = { TextButton(onClick = {
                onDeleteTrack(trackId)
                trackToDelete = null
            }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Eliminar")
            } },
            dismissButton = { TextButton(onClick = { trackToDelete = null }) { Text("Cancelar") } }
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
