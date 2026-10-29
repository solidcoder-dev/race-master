package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.solidcoder.trackchecker.routes.domain.model.DetectionRadius
import org.solidcoder.trackchecker.routes.domain.model.OverrideUpdate
import org.solidcoder.trackchecker.routes.domain.model.PointChanges

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointEditorSheet(
    point: RoutePoint,
    suggestedName: String?,
    defaultRadius: DetectionRadius,
    defaultScore: Int?,
    onDismiss: () -> Unit,
    onSave: (String, RoutePointType, PointChanges?) -> Unit
) {
    var name by remember(point.id, point.name) { mutableStateOf(point.name) }
    var type by remember(point.id, point.type) { mutableStateOf(point.type) }
    var menuExpanded by remember { mutableStateOf(false) }
    var customScore by remember(point.id, point.scoreValueOverride) { mutableStateOf(point.scoreValueOverride != null) }
    var scoreText by remember(point.id, point.scoreValueOverride, defaultScore) {
        mutableStateOf((point.scoreValueOverride ?: defaultScore ?: 10).toString())
    }
    var customRadius by remember(point.id, point.detectionRadiusOverride) { mutableStateOf(point.detectionRadiusOverride != null) }
    var radiusText by remember(point.id, point.detectionRadiusOverride) {
        mutableStateOf((point.detectionRadiusOverride ?: defaultRadius).meters.formatMeters())
    }
    val scoreable = type == RoutePointType.WAYPOINT || type == RoutePointType.CHECKPOINT
    val scoreValid = !scoreable || !customScore || (scoreText.toIntOrNull()?.let { it >= 0 } == true)
    val radiusValid = !customRadius || (radiusText.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).imePadding()
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
            if (suggestedName != null && name == point.name) {
                TextButton(onClick = { name = suggestedName }) {
                    Text("Usar nombre disponible: $suggestedName")
                }
            }
            Box {
                OutlinedButton(onClick = { menuExpanded = true }) {
                    Text("Tipo: ${type.displayName()}  ▾")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    RoutePointType.entries.forEach { candidate ->
                        DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    PointTypeDot(candidate)
                                    Text(candidate.displayName())
                                }
                            },
                            onClick = {
                                type = candidate
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
            HorizontalDivider()
            Text("Puntuación", style = MaterialTheme.typography.titleSmall)
            if (scoreable) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = customScore, onCheckedChange = { customScore = it })
                    Text("Personalizar valor del punto")
                }
                if (customScore) {
                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = { scoreText = it },
                        label = { Text("Puntos") },
                        isError = !scoreValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("El patrón de la ruta decide cuándo sumar o restar este valor.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Los patrones actuales no puntúan este tipo de punto.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Text("Radio de detección", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = customRadius, onCheckedChange = { customRadius = it })
                Text("Personalizar radio (base: ${defaultRadius.meters.formatMeters()} m)")
            }
            if (customRadius) {
                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it },
                    label = { Text("Radio en metros") },
                    isError = !radiusValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                "Coordenadas: ${point.coordinates.latitude}, ${point.coordinates.longitude}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val score = if (scoreable && customScore) scoreText.toInt() else null
                    val radius = if (customRadius) DetectionRadius(radiusText.toDouble()) else null
                    val scoreUpdate = if (scoreable && score != point.scoreValueOverride)
                        OverrideUpdate.Set(score) else OverrideUpdate.Keep
                    val radiusUpdate = if (radius != point.detectionRadiusOverride) OverrideUpdate.Set(radius) else OverrideUpdate.Keep
                    val changes = if (scoreUpdate != OverrideUpdate.Keep || radiusUpdate != OverrideUpdate.Keep)
                        PointChanges(score = scoreUpdate, radius = radiusUpdate) else null
                    onSave(name.trim(), type, changes)
                }, enabled = name.isNotBlank() && scoreValid && radiusValid) {
                    Text("Guardar cambios")
                }
            }
        }
    }
}

internal fun Double.formatMeters(): String = if (this == toLong().toDouble()) toLong().toString() else toString()

fun RoutePointType.displayName(): String = when (this) {
    RoutePointType.START -> "Salida"
    RoutePointType.FINISH -> "Meta"
    RoutePointType.WAYPOINT -> "Waypoint"
    RoutePointType.ECLIPSE_WAYPOINT -> "WPE"
    RoutePointType.CHECKPOINT -> "Checkpoint"
    RoutePointType.OTHER -> "Otro"
}

