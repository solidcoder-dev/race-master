package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.*

private enum class BulkEditMode(val label: String) {
    KEEP("Sin cambios"), DEFAULT("Restablecer al valor de la ruta"), CUSTOM("Valor personalizado")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkPointEditorSheet(
    count: Int,
    onDismiss: () -> Unit,
    onSave: (PointChanges) -> Unit
) {
    var editType by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(RoutePointType.WAYPOINT) }
    var typeMenu by remember { mutableStateOf(false) }
    var scoreMode by remember { mutableStateOf(BulkEditMode.KEEP) }
    var radiusMode by remember { mutableStateOf(BulkEditMode.KEEP) }
    var scoreText by remember { mutableStateOf("10") }
    var radiusText by remember { mutableStateOf("50") }
    val scoreValid = scoreMode != BulkEditMode.CUSTOM || (scoreText.toIntOrNull()?.let { it >= 0 } == true)
    val radiusValid = radiusMode != BulkEditMode.CUSTOM || (radiusText.toDoubleOrNull()?.let { it > 0 && it.isFinite() } == true)
    val hasChanges = editType || scoreMode != BulkEditMode.KEEP || radiusMode != BulkEditMode.KEEP

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Editar $count puntos", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = editType, onCheckedChange = { editType = it })
                Text("Cambiar tipo")
                Spacer(Modifier.width(12.dp))
                Box {
                    OutlinedButton(onClick = { typeMenu = true }, enabled = editType) {
                        Text("${type.displayName()} ▾")
                    }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        RoutePointType.entries.forEach { candidate ->
                            DropdownMenuItem(text = { Text(candidate.displayName()) }, onClick = {
                                type = candidate
                                typeMenu = false
                            })
                        }
                    }
                }
            }
            HorizontalDivider()
            Text("Puntuación", style = MaterialTheme.typography.titleSmall)
            BulkModeSelector(scoreMode, onChange = { scoreMode = it })
            if (scoreMode == BulkEditMode.CUSTOM) {
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = { Text("Valor en puntos") },
                    isError = !scoreValid, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            HorizontalDivider()
            Text("Radio de detección", style = MaterialTheme.typography.titleSmall)
            BulkModeSelector(radiusMode, onChange = { radiusMode = it })
            if (radiusMode == BulkEditMode.CUSTOM) {
                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it },
                    label = { Text("Radio en metros") },
                    isError = !radiusValid, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = hasChanges && scoreValid && radiusValid &&
                        !(editType && (type == RoutePointType.START || type == RoutePointType.FINISH) && count > 1),
                    onClick = {
                        onSave(PointChanges(
                            type = type.takeIf { editType },
                            score = when (scoreMode) {
                                BulkEditMode.KEEP -> OverrideUpdate.Keep
                                BulkEditMode.DEFAULT -> OverrideUpdate.Set(null)
                                BulkEditMode.CUSTOM -> OverrideUpdate.Set(scoreText.toInt())
                            },
                            radius = when (radiusMode) {
                                BulkEditMode.KEEP -> OverrideUpdate.Keep
                                BulkEditMode.DEFAULT -> OverrideUpdate.Set(null)
                                BulkEditMode.CUSTOM -> OverrideUpdate.Set(DetectionRadius(radiusText.toDouble()))
                            }
                        ))
                    }
                ) { Text("Aplicar a $count puntos") }
            }
        }
    }
}

@Composable
private fun BulkModeSelector(value: BulkEditMode, onChange: (BulkEditMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("${value.label} ▾") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BulkEditMode.entries.forEach { choice ->
                DropdownMenuItem(text = { Text(choice.label) }, onClick = {
                    onChange(choice)
                    expanded = false
                })
            }
        }
    }
}
