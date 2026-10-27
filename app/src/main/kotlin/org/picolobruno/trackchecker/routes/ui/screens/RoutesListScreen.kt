package org.picolobruno.trackchecker.routes.ui.screens

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
import org.picolobruno.trackchecker.routes.domain.model.Route
import org.picolobruno.trackchecker.routes.domain.model.RouteId
import org.picolobruno.trackchecker.routes.domain.model.RouteStatus
import org.picolobruno.trackchecker.routes.ui.RoutesUiState

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
                        route = route,
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

@Composable
private fun RouteCard(route: Route, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("◈", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text(route.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${route.points.size} puntos", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (route.status == RouteStatus.READY) "Lista" else "Revisar",
                style = MaterialTheme.typography.labelSmall,
                color = if (route.status == RouteStatus.READY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Box {
                TextButton(onClick = { menuExpanded = true }) { Text("⋮") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Renombrar") }, onClick = {
                        menuExpanded = false
                        onRename()
                    })
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = {
                        menuExpanded = false
                        onDelete()
                    })
                }
            }
        }
    }
}
