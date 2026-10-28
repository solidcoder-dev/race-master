package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointEditorSheet(
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

fun RoutePointType.displayName(): String = when (this) {
    RoutePointType.START -> "Salida"
    RoutePointType.FINISH -> "Meta"
    RoutePointType.WAYPOINT -> "Waypoint"
    RoutePointType.ECLIPSE_WAYPOINT -> "WPE"
    RoutePointType.CHECKPOINT -> "Checkpoint"
    RoutePointType.OTHER -> "Otro"
}

