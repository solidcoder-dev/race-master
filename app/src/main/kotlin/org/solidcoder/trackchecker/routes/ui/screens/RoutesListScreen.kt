package org.solidcoder.trackchecker.routes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RouteStatus
import org.solidcoder.trackchecker.routes.ui.RoutesUiState
import org.solidcoder.trackchecker.routes.ui.components.RouteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesListScreen(
    state: RoutesUiState,
    onImport: () -> Unit,
    onOpen: (RouteId) -> Unit,
    onRename: (RouteId, String) -> Unit,
    onDelete: (RouteId) -> Unit,
    modifier: Modifier = Modifier
) {
    var routeToRename by remember { mutableStateOf<Route?>(null) }
    var routeToDelete by remember { mutableStateOf<Route?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mis rutas") },
                actions = {
                    if (state.routes.isNotEmpty()) {
                        TextButton(onClick = onImport, enabled = !state.importing) {
                            Text("+ Importar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.routes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Todavía no hay rutas", style = MaterialTheme.typography.titleLarge)
                    Text("Importa tu primera ruta para empezar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onImport, enabled = !state.importing) {
                        Text(if (state.importing) "Importando..." else "Importar ruta")
                    }
                    Text("KML · GPX · KMZ", style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.routes, key = { it.id.value }) { route ->
                    RouteCard(
                        name = route.name,
                        pointCount = route.points.size,
                        isReady = route.status == RouteStatus.READY,
                        onOpen = { onOpen(route.id) },
                        onRename = { routeToRename = route },
                        onDelete = { routeToDelete = route }
                    )
                }
            }
        }
    }

    routeToRename?.let { route ->
        var value by remember(route.id) { mutableStateOf(route.name) }
        AlertDialog(
            onDismissRequest = { routeToRename = null },
            title = { Text("Renombrar ruta") },
            text = {
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Nombre") })
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(route.id, value)
                    routeToRename = null
                }, enabled = value.isNotBlank()) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { routeToRename = null }) { Text("Cancelar") } }
        )
    }

    routeToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = { Text("¿Eliminar esta ruta?") },
            text = { Text("Se eliminarán «${route.name}» y sus ${route.points.size} puntos. El archivo original no se modifica.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(route.id)
                    routeToDelete = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Eliminar")
                }
            },
            dismissButton = { TextButton(onClick = { routeToDelete = null }) { Text("Cancelar") } }
        )
    }
}
