package org.solidcoder.trackchecker.scoring.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.ui.components.PointTypeDot
import org.solidcoder.trackchecker.scoring.application.usecases.EvaluatedTrackSummary
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus
import org.solidcoder.trackchecker.scoring.domain.model.PointPassage
import org.solidcoder.trackchecker.tracks.domain.model.TrackId

@Composable
fun TrackSelectorRow(
    routeId: RouteId,
    tracks: List<EvaluatedTrackSummary>,
    selected: EvaluatedTrackSummary?,
    routeReady: Boolean,
    onSelect: (TrackId?) -> Unit
) {
    var expanded by remember(routeId) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected?.track?.name ?: "Ruta oficial", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(" ⌄")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Ruta oficial") }, onClick = {
                    expanded = false
                    onSelect(null)
                })
                tracks.forEach { item ->
                    DropdownMenuItem(text = {
                        Text("${item.track.name} · ${item.evaluation.score?.total?.let { "$it pts" } ?: "Sin puntuación"}")
                    }, onClick = {
                        expanded = false
                        onSelect(item.track.id)
                    })
                }
            }
        }
        Text(
            if (selected != null) "Evaluado" else if (routeReady) "Lista" else "Borrador",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TrackEvaluationSummary(
    track: EvaluatedTrackSummary,
    route: Route,
    routeReady: Boolean,
    loadingGeometry: Boolean,
    onRecalculate: (TrackId) -> Unit
) {
    val evaluation = track.evaluation
    val reached = evaluation.passageReport.passages.count { it.status == PassageStatus.PASSED }
    val missed = evaluation.passageReport.passages.count { it.status == PassageStatus.MISSED }
    Text(
        "${evaluation.score?.total?.let { "$it pts" } ?: "Sin puntuación"} · $reached alcanzados · $missed no alcanzados",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    when {
        evaluation.isOutdatedFor(route) -> {
            Text(
                "Resultado desactualizado: han cambiado las reglas de la ruta",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            if (routeReady) {
                TextButton(onClick = { onRecalculate(track.track.id) }) { Text("Recalcular") }
            }
        }
        evaluation.passageReport.needsReview -> {
            Text(
                "Requiere revisión de salida, meta o muestras GPS",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    if (loadingGeometry) {
        Text("Cargando recorrido GPS…", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun TrackPointResultRow(
    point: RoutePoint,
    passage: PointPassage?,
    contribution: Int?,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PointTypeDot(point.type)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(point.name, fontWeight = FontWeight.Medium)
            Text(
                when (passage?.status) {
                    PassageStatus.PASSED -> "Alcanzado"
                    PassageStatus.MISSED -> "No alcanzado"
                    PassageStatus.UNCERTAIN -> "Paso indeterminado"
                    null -> "Sin evaluación"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        contribution?.let { value ->
            Text(if (value > 0) "+$value pts" else "$value pts", style = MaterialTheme.typography.labelMedium)
        }
        Text(" ›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
}

@Composable
fun TrackPointDetailsDialog(
    point: RoutePoint,
    passage: PointPassage,
    contribution: Int?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(point.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estado: " + when (passage.status) {
                    PassageStatus.PASSED -> "Alcanzado"
                    PassageStatus.MISSED -> "No alcanzado"
                    PassageStatus.UNCERTAIN -> "Indeterminado"
                })
                Text("Distancia mínima: ${String.format(Locale.ROOT, "%.0f", passage.minimumDistanceMeters)} m")
                Text("Radio de detección: ${String.format(Locale.ROOT, "%.0f", passage.detectionRadiusMeters)} m")
                contribution?.let { Text("Puntuación: $it pts") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
