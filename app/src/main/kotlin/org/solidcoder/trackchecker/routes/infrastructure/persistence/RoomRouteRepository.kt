package org.solidcoder.trackchecker.routes.infrastructure.persistence

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.domain.repositories.RouteRepository
import org.solidcoder.trackchecker.routes.application.ports.RouteCatalog

class RoomRouteRepository(private val database: RoutesDatabase) : RouteRepository, RouteCatalog {
    private val dao = database.routes()

    override fun observeRoutes(): Flow<List<Route>> = dao.observe().map { rows ->
        rows.map { it.toDomain() }
    }

    override suspend fun findById(id: RouteId): Route? = dao.findById(id.value)?.toDomain()

    override suspend fun save(route: Route) {
        database.withTransaction {
            dao.upsertRoute(
                RouteRecord(route.id.value, route.name, route.status.name, route.importedAtEpochMs,
                    route.defaultDetectionRadius.meters, route.scoringPattern?.name)
            )
            // Points retain their identity across route and point edits.
            dao.upsertPoints(route.points.mapIndexed { index, point ->
                RoutePointRecord(
                    id = point.id.value,
                    routeId = route.id.value,
                    sequence = index,
                    name = point.name,
                    latitude = point.coordinates.latitude,
                    longitude = point.coordinates.longitude,
                    type = point.type.name,
                    detectionRadiusOverrideMeters = point.detectionRadiusOverride?.meters,
                    scoreValueOverride = point.scoreValueOverride
                )
            })
        }
    }

    override suspend fun delete(id: RouteId) = dao.deleteRoute(id.value)

    private fun RouteWithPoints.toDomain(): Route = Route.restore(
        id = RouteId(route.id),
        name = route.name,
        points = points.sortedBy(RoutePointRecord::sequence).map { record ->
            RoutePoint(
                id = RoutePointId(record.id),
                name = record.name,
                coordinates = Coordinates(record.latitude, record.longitude),
                type = RoutePointType.valueOf(record.type),
                detectionRadiusOverride = record.detectionRadiusOverrideMeters?.let(::DetectionRadius),
                scoreValueOverride = record.scoreValueOverride
            )
        },
        status = RouteStatus.valueOf(route.status),
        importedAtEpochMs = route.importedAtEpochMs,
        defaultDetectionRadius = DetectionRadius(route.defaultDetectionRadiusMeters),
        scoringPattern = route.scoringPattern?.let(ScoringPattern::valueOf)
    )
}
