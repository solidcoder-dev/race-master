package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.*

/** Presentation of structured validation issues; localization belongs to UI. */
@Composable
fun RouteIssues(
    route: Route,
    issues: List<RouteIssue>,
    onSelectPoint: (RoutePointId) -> Unit
) {
    issues.forEach { issue ->
        key(issue.code, issue.pointIds) { RouteIssueCard(route, issue, onSelectPoint) }
    }
}

@Composable
private fun RouteIssueCard(
    route: Route,
    issue: RouteIssue,
    onSelectPoint: (RoutePointId) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = if (issue.severity == IssueSeverity.ERROR)
            MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(
                Modifier.fillMaxWidth().then(
                    if (issue.pointIds.isNotEmpty()) Modifier.clickable { expanded = !expanded } else Modifier
                )
            ) {
                Text(issue.displayMessage(route), fontWeight = FontWeight.Medium)
                if (issue.pointIds.isNotEmpty()) {
                    Text(
                        "${issue.pointIds.size} puntos afectados · ${if (expanded) "Ocultar" else "Ver puntos"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (expanded) {
                issue.pointIds.forEach { pointId ->
                    val point = route.points.firstOrNull { it.id == pointId } ?: return@forEach
                    TextButton(onClick = { onSelectPoint(pointId) }) {
                        Text("${point.name} · %.4f, %.4f".format(
                            java.util.Locale.ROOT, point.coordinates.latitude, point.coordinates.longitude
                        ))
                    }
                }
            }
        }
    }
}

private fun RouteIssue.displayMessage(route: Route): String = when (code) {
    RouteIssueCode.NO_POINTS -> "La ruta no contiene puntos"
    RouteIssueCode.MISSING_START -> "Identifica la salida"
    RouteIssueCode.MULTIPLE_START -> "Hay varias salidas"
    RouteIssueCode.MISSING_FINISH -> "Identifica la meta"
    RouteIssueCode.MULTIPLE_FINISH -> "Hay varias metas"
    RouteIssueCode.MISSING_WAYPOINT -> "Identifica al menos un waypoint"
    RouteIssueCode.MISSING_CHECKPOINT -> "Identifica al menos un checkpoint"
    RouteIssueCode.UNKNOWN_POINT_TYPE -> "Clasifica los puntos sin tipo"
    RouteIssueCode.DUPLICATE_NAME -> {
        val name = pointIds.firstOrNull()?.let { id -> route.points.firstOrNull { it.id == id }?.name }
        if (name == null) "Hay puntos con nombre repetido" else "Nombre repetido: $name"
    }
}
