package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.DetectionRadius
import org.solidcoder.trackchecker.routes.domain.model.ScoringPattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSettingsSheet(
    currentRadius: DetectionRadius,
    onDismiss: () -> Unit,
    onSave: (DetectionRadius) -> Unit
) {
    var radius by remember(currentRadius) { mutableStateOf(currentRadius.meters.formatMeters()) }
    val parsed = radius.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).imePadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Configuración de la ruta", style = MaterialTheme.typography.titleLarge)
            Text("Radio predeterminado", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it },
                label = { Text("Metros") },
                supportingText = { Text("Se utiliza en todos los puntos que no tienen radio personalizado.") },
                singleLine = true, isError = parsed == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(enabled = parsed != null, onClick = { parsed?.let { onSave(DetectionRadius(it)) } }) {
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
fun ScoringPatternDialog(
    current: ScoringPattern?,
    overrideCount: Int,
    onDismiss: () -> Unit,
    onApply: (ScoringPattern, Boolean) -> Unit
) {
    var selected by remember(current) { mutableStateOf(current ?: ScoringPattern.MIXED) }
    var replaceOverrides by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar patrón de puntuación") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Elige una regla sugerida para toda la ruta. Puedes ajustar puntos concretos después.",
                    style = MaterialTheme.typography.bodyMedium)
                ScoringPattern.entries.forEach { pattern ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = pattern },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == pattern, onClick = { selected = pattern })
                        Column {
                            Text(pattern.label(), style = MaterialTheme.typography.titleSmall)
                            Text(pattern.description(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                HorizontalDivider()
                Text("Vista previa: ${selected.description()}", style = MaterialTheme.typography.bodySmall)
                Text("Salida, meta y WPE no tienen puntuación automática.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (overrideCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = replaceOverrides, onCheckedChange = { replaceOverrides = it })
                        Text("Reemplazar $overrideCount valores personalizados", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(selected, replaceOverrides) }) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

fun ScoringPattern.label(): String = when (this) {
    ScoringPattern.PASSED_POINTS -> "Premiar pasos"
    ScoringPattern.MISSED_PENALTIES -> "Penalizar omisiones"
    ScoringPattern.MIXED -> "Mixto"
}

fun ScoringPattern.description(): String = when (this) {
    ScoringPattern.PASSED_POINTS -> "Waypoint superado +10 · Checkpoint omitido 0"
    ScoringPattern.MISSED_PENALTIES -> "Waypoint superado 0 · Checkpoint omitido −20"
    ScoringPattern.MIXED -> "Waypoint superado +10 · Checkpoint omitido −20"
}
