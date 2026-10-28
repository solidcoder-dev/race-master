package org.solidcoder.trackchecker.routes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.ui.RouteReviewUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteReviewScreen(
    state: RouteReviewUiState,
    onBack: () -> Unit,
    onEdit: (RoutePointId, String, RoutePointType) -> Unit,
    onValidate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val route = state.route
    if (route == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var editingPointId by remember(route.id) { mutableStateOf<RoutePointId?>(null) }
    val selectedPoint = route.points.firstOrNull { it.id == editingPointId }
    val sheetScaffold = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = sheetScaffold,
        sheetPeekHeight = 218.dp,
        topBar = {
            TopAppBar(
                title = { Text(route.name, maxLines = 1) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        },
        sheetContent = {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${route.points.size} puntos · ${state.issues.size} incidencias",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                state.issues.forEach { issue ->
                    Surface(
                        color = if (issue.severity == IssueSeverity.ERROR)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    issue.pointIds.firstOrNull()?.let { editingPointId = it }
                                }.padding(12.dp)
                        ) {
                            Text(issue.message, fontWeight = FontWeight.Medium)
                            if (issue.pointIds.isNotEmpty()) {
                                Text("${issue.pointIds.size} puntos afectados", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                TextButton(onClick = { scope.launch { sheetScaffold.bottomSheetState.expand() } }) {
                    Text("Ver todos los puntos")
                }
                Button(
                    onClick = onValidate,
                    enabled = route.status != RouteStatus.READY,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (route.status == RouteStatus.READY) "Ruta lista" else "Validar ruta")
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(route.points, key = { it.id.value }) { point ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { editingPointId = point.id }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(point.name, fontWeight = FontWeight.Medium)
                                Text(point.type.displayName(), style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Editar", color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    ) { contentPadding ->
        RouteMap(
            route = route,
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            onPointClick = { editingPointId = it }
        )
    }

    selectedPoint?.let { point ->
        PointEditorSheet(
            point = point,
            onDismiss = { editingPointId = null },
            onSave = { name, type ->
                onEdit(point.id, name, type)
                editingPointId = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointEditorSheet(
    point: RoutePoint,
    onDismiss: () -> Unit,
    onSave: (String, RoutePointType) -> Unit
) {
    var name by remember(point.id, point.name) { mutableStateOf(point.name) }
    var type by remember(point.id, point.type) { mutableStateOf(point.type) }
    var menuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).imePadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Editar punto", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Box {
                OutlinedButton(onClick = { menuExpanded = true }) {
                    Text("Tipo: ${type.displayName()}  ▾")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    RoutePointType.entries.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.displayName()) },
                            onClick = {
                                type = candidate
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
            Text(
                "Coordenadas: ${point.coordinates.latitude}, ${point.coordinates.longitude}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(name.trim(), type) }, enabled = name.isNotBlank()) {
                    Text("Guardar cambios")
                }
            }
        }
    }
}

private fun RoutePointType.displayName(): String = when (this) {
    RoutePointType.START -> "Salida"
    RoutePointType.FINISH -> "Meta"
    RoutePointType.WAYPOINT -> "Waypoint"
    RoutePointType.EXIT_WAYPOINT -> "WPE"
    RoutePointType.CHECKPOINT -> "Checkpoint"
    RoutePointType.OTHER -> "Otro"
}
