package org.solidcoder.trackchecker.routes.domain.repositories

import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteId

interface RouteRepository {
    suspend fun findById(id: RouteId): Route?
    suspend fun save(route: Route)
    suspend fun delete(id: RouteId)
}
