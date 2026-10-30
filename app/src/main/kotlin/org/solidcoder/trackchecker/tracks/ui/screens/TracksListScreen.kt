package org.solidcoder.trackchecker.tracks.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.scoring.application.usecases.EvaluatedTrackSummary
import org.solidcoder.trackchecker.scoring.domain.model.EvaluationStatus
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus
import org.solidcoder.trackchecker.scoring.ui.sortTrackSummaries
import org.solidcoder.trackchecker.tracks.domain.model.TrackId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksListScreen(
    route: Route,
    tracks: List<EvaluatedTrackSummary>,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onOpen: (TrackId) -> Unit,
    modifier: Modifier = Modifier
) {
    val sorted = sortTrackSummaries(route, tracks)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Tracks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = { TextButton(onClick = onImport) { Text("+ Importar") } }
            )
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Text(
                "${tracks.size} recorridos evaluados",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(Modifier.fillMaxWidth()) {
                itemsIndexed(sorted, key = { _, item -> item.track.id.value }) { index, item ->
                    TrackClassificationRow(index + 1, route, item, onClick = { onOpen(item.track.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TrackClassificationRow(
    rank: Int,
    route: Route,
    item: EvaluatedTrackSummary,
    onClick: () -> Unit
) {
    val evaluation = item.evaluation
    val reached = evaluation.passageReport.passages.count { it.status == PassageStatus.PASSED }
    val missed = evaluation.passageReport.passages.count { it.status == PassageStatus.MISSED }
    val score = evaluation.score?.total?.let { "$it pts" } ?: "Sin puntuación"
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("$rank", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.track.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium)
            Text("$reached alcanzados · $missed no alcanzados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                evaluation.isOutdatedFor(route) -> Text("Desactualizado", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
                evaluation.status == EvaluationStatus.NEEDS_REVIEW -> Text("Requiere revisión", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(score, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
