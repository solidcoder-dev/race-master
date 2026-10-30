package org.solidcoder.trackchecker.scoring.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.ui.components.RouteMap
import org.solidcoder.trackchecker.scoring.domain.model.PassageStatus
import org.solidcoder.trackchecker.scoring.domain.model.TrackEvaluation
import org.solidcoder.trackchecker.tracks.domain.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMapScreen(
    route: Route,
    trackName: String,
    track: Track?,
    evaluation: TrackEvaluation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(trackName, maxLines = 1) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { insets ->
        if (track == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            RouteMap(
                routeId = route.id,
                points = route.points,
                defaultRadius = route.defaultDetectionRadius,
                showDetectionRadii = false,
                selectedPointId = null,
                selectedPointIds = emptySet(),
                track = track,
                passageStatuses = evaluation.passageReport.passages.associate { it.pointId to it.status },
                onPointClick = {},
                onPointLongPress = {},
                modifier = Modifier.fillMaxSize().padding(insets)
            )
        }
    }
}
