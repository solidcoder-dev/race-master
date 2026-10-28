package org.solidcoder.trackchecker.routes.application.ports

import kotlinx.coroutines.flow.Flow
import org.solidcoder.trackchecker.routes.domain.model.Route

/** Observable catalog of routes for presentation and application queries. */
fun interface RouteCatalog {
    fun observeRoutes(): Flow<List<Route>>
}
