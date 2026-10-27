package org.picolobruno.trackchecker.routes.application.usecases

import kotlinx.coroutines.flow.Flow
import org.picolobruno.trackchecker.routes.domain.model.Route
import org.picolobruno.trackchecker.routes.domain.model.RouteId
import org.picolobruno.trackchecker.routes.domain.model.RoutePointId
import org.picolobruno.trackchecker.routes.domain.model.RoutePointType
import org.picolobruno.trackchecker.routes.domain.model.RouteValidation

fun interface ObserveRoutesUseCase { operator fun invoke(): Flow<List<Route>> }
fun interface GetRouteUseCase { suspend operator fun invoke(id: RouteId): Route? }
fun interface ImportRouteUseCase { suspend operator fun invoke(fileName: String, content: ByteArray): Route }
fun interface RenameRouteUseCase { suspend operator fun invoke(id: RouteId, newName: String) }
fun interface UpdateRoutePointUseCase {
    suspend operator fun invoke(id: RouteId, pointId: RoutePointId, name: String, type: RoutePointType)
}
fun interface ValidateRouteUseCase { suspend operator fun invoke(id: RouteId): RouteValidation }
fun interface DeleteRouteUseCase { suspend operator fun invoke(id: RouteId) }
