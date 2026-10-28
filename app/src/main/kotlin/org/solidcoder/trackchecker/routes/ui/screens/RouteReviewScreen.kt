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
import org.solidcoder.trackchecker.routes.ui.components.PointEditorSheet
import org.solidcoder.trackchecker.routes.ui.components.RouteIssues
import org.solidcoder.trackchecker.routes.ui.components.RouteMap
import org.solidcoder.trackchecker.routes.ui.components.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteReviewScreen(
    state: RouteReviewUiState,
    onBack: () -> Unit,
    onEdit: (RoutePointId, String, RoutePointType) -> Unit,
    onMarkReady: () -> Unit,
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
                RouteIssues(
                    route = route,
                    issues = state.issues,
                    onSelectPoint = { editingPointId = it }
                )
                TextButton(onClick = { scope.launch { sheetScaffold.bottomSheetState.expand() } }) {
                    Text("Ver todos los puntos")
                }
                Button(
                    onClick = onMarkReady,
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
