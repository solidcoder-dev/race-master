package org.solidcoder.trackchecker.scoring.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.ui.components.PointTypeDot
import org.solidcoder.trackchecker.routes.ui.components.displayName
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus
import org.solidcoder.trackchecker.scoring.domain.model.PointPassage
import org.solidcoder.trackchecker.scoring.domain.model.TrackEvaluation
import org.solidcoder.trackchecker.scoring.ui.TrackPointFilter
import org.solidcoder.trackchecker.scoring.ui.filterTrackPoints

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    route: Route,
    trackName: String,
    evaluation: TrackEvaluation,
    onBack: () -> Unit,
    onMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(TrackPointFilter.ALL) }
    val passages = evaluation.passageReport.passages.associateBy { it.pointId }
    val contributions = evaluation.score?.contributions?.associateBy { it.pointId }.orEmpty()
    val passed = evaluation.passageReport.passages.count { it.status == PassageStatus.PASSED }
    val missed = evaluation.passageReport.passages.count { it.status == PassageStatus.MISSED }
    val points = filterTrackPoints(route, evaluation, filter)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(trackName, maxLines = 1) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
                actions = { TextButton(onClick = onMap) { Text("Mapa") } }
            )
        }
    ) { insets ->
        LazyColumn(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(evaluation.score?.let { "${it.total} pts" } ?: "Sin puntuación",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("$passed alcanzados · $missed no alcanzados",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (evaluation.passageReport.needsReview) {
                        Text("Requiere revisión", color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TrackPointFilter.ALL to "Todos",
                        TrackPointFilter.PASSED to "Alcanzados",
                        TrackPointFilter.MISSED to "No alcanzados",
                        TrackPointFilter.UNCERTAIN to "Dudosos"
                    ).forEach { (candidate, label) ->
                        FilterChip(selected = filter == candidate, onClick = { filter = candidate }, label = { Text(label) })
                    }
                }
            }
            items(points, key = { it.id.value }) { point ->
                TrackDetailPointRow(point, passages[point.id], contributions[point.id]?.points)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TrackDetailPointRow(point: RoutePoint, passage: PointPassage?, contribution: Int?) {
    val status = passage?.status
    val statusText = when (status) {
        PassageStatus.PASSED -> "✓ Alcanzado"
        PassageStatus.MISSED -> "✕ No alcanzado"
        PassageStatus.UNCERTAIN -> "? Indeterminado"
        null -> "? Sin evaluación"
    }
    val distance = passage?.let {
        "${formatMeters(it.minimumDistanceMeters)} · Radio ${formatMeters(it.detectionRadiusMeters)}"
    } ?: "Requiere revisión"
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        PointTypeDot(point.type)
        Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${point.name} · ${point.type.displayName()}", fontWeight = FontWeight.Medium)
            Text(statusText, color = when (status) {
                PassageStatus.PASSED -> MaterialTheme.colorScheme.primary
                PassageStatus.MISSED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.tertiary
            })
            Text(distance, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        contribution?.let {
            Text(if (it > 0) "+$it pts" else "$it pts", fontWeight = FontWeight.SemiBold)
        } ?: Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatMeters(value: Double): String =
    String.format(Locale.ROOT, "%.0f m", value)
