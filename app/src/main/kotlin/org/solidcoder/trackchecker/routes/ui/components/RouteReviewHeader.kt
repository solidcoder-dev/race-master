package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteReviewHeader(
    routeName: String,
    selectionCount: Int,
    showDetectionRadii: Boolean,
    onNavigate: () -> Unit,
    onEditSelected: () -> Unit,
    onToggleRadii: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPatterns: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(
            if (selectionCount > 0) "$selectionCount seleccionados" else routeName,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        ) },
        navigationIcon = {
            TextButton(onClick = onNavigate) { Text(if (selectionCount > 0) "×" else "←") }
        },
        actions = {
            if (selectionCount > 0) {
                IconButton(onClick = onEditSelected, modifier = Modifier.semantics {
                    contentDescription = "Editar puntos seleccionados"
                }) { Text("✎", style = MaterialTheme.typography.titleLarge) }
            } else {
                IconButton(onClick = onToggleRadii, modifier = Modifier.semantics {
                    contentDescription = if (showDetectionRadii) "Ocultar radios" else "Mostrar radios"
                }) {
                    Text("◎", style = MaterialTheme.typography.titleLarge,
                        color = if (showDetectionRadii) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.semantics {
                        contentDescription = "Opciones de ruta"
                    }) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Configuración de ruta") }, onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        })
                        DropdownMenuItem(text = { Text("Aplicar patrón de puntuación") }, onClick = {
                            menuExpanded = false
                            onOpenPatterns()
                        })
                    }
                }
            }
        }
    )
}
