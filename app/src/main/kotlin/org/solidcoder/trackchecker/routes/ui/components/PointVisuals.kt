package org.solidcoder.trackchecker.routes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType

/** Visual semantics only; point types and their business meaning remain in domain. */
internal object PointVisuals {
    fun color(type: RoutePointType): Int = when (type) {
        RoutePointType.START -> 0xFF16A34A.toInt()
        RoutePointType.FINISH -> 0xFFDC2626.toInt()
        RoutePointType.WAYPOINT -> 0xFF2563EB.toInt()
        RoutePointType.CHECKPOINT -> 0xFFD97706.toInt()
        RoutePointType.ECLIPSE_WAYPOINT -> 0xFF9333EA.toInt()
        RoutePointType.OTHER -> 0xFF64748B.toInt()
    }

    fun symbol(type: RoutePointType): String = when (type) {
        RoutePointType.START -> "S"
        RoutePointType.FINISH -> "F"
        RoutePointType.WAYPOINT -> "W"
        RoutePointType.CHECKPOINT -> "C"
        RoutePointType.ECLIPSE_WAYPOINT -> "E"
        RoutePointType.OTHER -> "?"
    }
}

@Composable
internal fun PointTypeDot(type: RoutePointType) {
    Box(Modifier.size(10.dp).background(Color(PointVisuals.color(type)), CircleShape))
}
