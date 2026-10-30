package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TracksEntryCopy(val title: String, val summary: String, val action: String)

fun tracksEntryCopy(trackCount: Int): TracksEntryCopy = if (trackCount == 0) {
    TracksEntryCopy("Tracks", "Todavía no hay recorridos", "Importar tracks")
} else {
    TracksEntryCopy("Tracks", "$trackCount recorridos evaluados", "Abrir")
}

@Composable
fun RouteTracksEntry(
    trackCount: Int,
    routeReady: Boolean,
    canValidate: Boolean,
    onOpen: () -> Unit,
    onValidate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = tracksEntryCopy(trackCount)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = routeReady, onClick = onOpen)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(copy.title, fontWeight = FontWeight.SemiBold)
                Text(copy.summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleLarge,
                color = if (routeReady) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trackCount == 0) {
            OutlinedButton(onClick = if (routeReady) onOpen else onValidate,
                enabled = routeReady || canValidate) {
                Text(if (routeReady) copy.action else "Validar ruta")
            }
        }
    }
}
