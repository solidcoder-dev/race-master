package org.solidcoder.trackchecker.routes.domain.model

import org.solidcoder.trackchecker.routes.domain.services.RouteValidator

enum class RouteStatus { DRAFT, READY }

/**
 * Aggregate root. Edits preserve point IDs and leave the route ready for revalidation.
 * No parsing, Android, or database implementation belongs here.
 */
class Route private constructor(
    val id: RouteId,
    val name: String,
    points: List<RoutePoint>,
    val status: RouteStatus,
    val importedAtEpochMs: Long
) {
    val points: List<RoutePoint> = points.toList()

    init {
        require(name.isNotBlank()) { "Route name cannot be blank" }
        require(points.map { it.id }.distinct().size == points.size) { "Point IDs must be unique" }
        require(importedAtEpochMs >= 0) { "Invalid import timestamp" }
    }

    companion object {
        fun imported(
            id: RouteId,
            name: String,
            points: List<RoutePoint>,
            importedAtEpochMs: Long
        ): Route = Route(id, name.trim(), points, RouteStatus.DRAFT, importedAtEpochMs)

        fun restore(
            id: RouteId,
            name: String,
            points: List<RoutePoint>,
            status: RouteStatus,
            importedAtEpochMs: Long
        ): Route = Route(id, name, points, status, importedAtEpochMs)
    }

    fun rename(newName: String): Route {
        require(newName.isNotBlank()) { "Route name cannot be blank" }
        return Route(id, newName.trim(), points, status, importedAtEpochMs)
    }

    fun renamePoint(pointId: RoutePointId, newName: String): Route =
        editPoint(pointId) { it.rename(newName) }

    fun changePointType(pointId: RoutePointId, newType: RoutePointType): Route =
        editPoint(pointId) { it.changeType(newType) }

    fun markReady(): Route {
        require(RouteValidator().validate(this).canBeReady) { "A route with blocking issues cannot be ready" }
        return Route(id, name, points, RouteStatus.READY, importedAtEpochMs)
    }

    private fun editPoint(pointId: RoutePointId, edit: (RoutePoint) -> RoutePoint): Route {
        require(points.any { it.id == pointId }) { "Point does not belong to this route" }
        return Route(
            id, name,
            points.map { if (it.id == pointId) edit(it) else it },
            RouteStatus.DRAFT,
            importedAtEpochMs
        )
    }
}
