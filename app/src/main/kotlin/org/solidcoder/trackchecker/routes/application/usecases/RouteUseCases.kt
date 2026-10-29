package org.solidcoder.trackchecker.routes.application.usecases

import kotlinx.coroutines.flow.Flow
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.solidcoder.trackchecker.routes.domain.model.RouteValidation
import org.solidcoder.trackchecker.routes.domain.model.DetectionRadius
import org.solidcoder.trackchecker.routes.domain.model.PointChanges
import org.solidcoder.trackchecker.routes.domain.model.ScoringPattern

data class EditRoutePointDetails(
    val routeId: RouteId,
    val pointId: RoutePointId,
    val name: String,
    val type: RoutePointType,
    val changes: PointChanges? = null
)

data class EditRoutePoints(
    val routeId: RouteId,
    val pointIds: Set<RoutePointId>,
    val changes: PointChanges
)

data class ApplyRouteScoringPattern(
    val routeId: RouteId,
    val pattern: ScoringPattern,
    val replaceOverrides: Boolean = false
)

fun interface ObserveRoutesUseCase { operator fun invoke(): Flow<List<Route>> }
fun interface GetRouteUseCase { suspend operator fun invoke(id: RouteId): Route? }
fun interface ImportRouteUseCase { suspend operator fun invoke(source: RouteImportSource): Route }
fun interface RenameRouteUseCase { suspend operator fun invoke(id: RouteId, newName: String) }
fun interface EditRoutePointDetailsUseCase {
    suspend operator fun invoke(details: EditRoutePointDetails)
}
fun interface EditRoutePointsUseCase { suspend operator fun invoke(command: EditRoutePoints) }
fun interface SetRouteDetectionRadiusUseCase {
    suspend operator fun invoke(routeId: RouteId, radius: DetectionRadius)
}
fun interface ApplyRouteScoringPatternUseCase {
    suspend operator fun invoke(command: ApplyRouteScoringPattern)
}
/** Returns issues without changing the stored route. */
fun interface ValidateRouteUseCase { suspend operator fun invoke(id: RouteId): RouteValidation }
/** Attempts to mark the route READY, returning issues when it cannot be confirmed. */
fun interface MarkRouteReadyUseCase { suspend operator fun invoke(id: RouteId): RouteValidation }
fun interface DeleteRouteUseCase { suspend operator fun invoke(id: RouteId) }
