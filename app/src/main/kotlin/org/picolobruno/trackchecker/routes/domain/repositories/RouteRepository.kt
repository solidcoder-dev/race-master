package org.picolobruno.trackchecker.routes.domain.repositories

import kotlinx.coroutines.flow.Flow
import org.picolobruno.trackchecker.routes.domain.model.Route
import org.picolobruno.trackchecker.routes.domain.model.RouteId

interface RouteRepository {
    fun observe(): Flow<List<Route>>
    suspend fun findById(id: RouteId): Route?
    suspend fun save(route: Route)
    suspend fun delete(id: RouteId)
}
