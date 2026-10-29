package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Reusable route summary; only local menu state lives inside the component. */
@Composable
fun RouteCard(
    name: String,
    pointCount: Int,
    isReady: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("◈", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${pointCount} puntos", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (isReady) "Lista" else "Revisar",
                style = MaterialTheme.typography.labelSmall,
                color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Box {
                TextButton(onClick = { menuExpanded = true }) {
                    Text("⋮", fontSize = 28.sp, lineHeight = 32.sp)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Renombrar") }, onClick = {
                        menuExpanded = false
                        onRename()
                    })
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = {
                        menuExpanded = false
                        onDelete()
                    })
                }
            }
        }
    }
}
