package org.solidcoder.trackchecker.routes.domain.model

import org.solidcoder.trackchecker.routes.domain.services.RouteValidator

enum class RouteStatus { DRAFT, READY }

/** A validation report and the resulting ready aggregate, when valid. */
data class RouteReadiness(val validation: RouteValidation, val readyRoute: Route?)

/**
 * Aggregate root. Edits preserve point IDs and leave the route ready for revalidation.
 * No parsing, Android, or database implementation belongs here.
 */
class Route private constructor(
    val id: RouteId,
    val name: String,
    points: List<RoutePoint>,
    val status: RouteStatus,
    val importedAtEpochMs: Long,
    val defaultDetectionRadius: DetectionRadius,
    val scoringPattern: ScoringPattern?
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
            importedAtEpochMs: Long,
            defaultDetectionRadius: DetectionRadius = DetectionRadius.DEFAULT,
            scoringPattern: ScoringPattern? = null
        ): Route = Route(id, name.trim(), points, RouteStatus.DRAFT, importedAtEpochMs, defaultDetectionRadius, scoringPattern)

        fun restore(
            id: RouteId,
            name: String,
            points: List<RoutePoint>,
            status: RouteStatus,
            importedAtEpochMs: Long,
            defaultDetectionRadius: DetectionRadius = DetectionRadius.DEFAULT,
            scoringPattern: ScoringPattern? = null
        ): Route = Route(id, name, points, status, importedAtEpochMs, defaultDetectionRadius, scoringPattern)
    }

    fun rename(newName: String): Route {
        require(newName.isNotBlank()) { "Route name cannot be blank" }
        return Route(id, newName.trim(), points, status, importedAtEpochMs, defaultDetectionRadius, scoringPattern)
    }

    fun detectionRadiusFor(pointId: RoutePointId): DetectionRadius {
        val point = points.firstOrNull { it.id == pointId }
            ?: throw IllegalArgumentException("Point does not belong to this route")
        return point.detectionRadiusOverride ?: defaultDetectionRadius
    }

    fun changeDefaultDetectionRadius(radius: DetectionRadius): Route =
        Route(id, name, points, RouteStatus.DRAFT, importedAtEpochMs, radius, scoringPattern)

    fun scoringFor(pointId: RoutePointId): PointScoreEffect? {
        val point = points.firstOrNull { it.id == pointId }
            ?: throw IllegalArgumentException("Point does not belong to this route")
        return scoringPattern?.effectFor(point.type, point.scoreValueOverride)
    }

    fun overridePointScore(pointId: RoutePointId, value: Int?): Route =
        editPoint(pointId) { it.withScoreValueOverride(value) }

    fun applyScoringPattern(pattern: ScoringPattern, replaceOverrides: Boolean = false): Route {
        val updatedPoints = if (replaceOverrides) points.map { it.withScoreValueOverride(null) } else points
        return Route(id, name, updatedPoints, RouteStatus.DRAFT, importedAtEpochMs, defaultDetectionRadius, pattern)
    }

    fun updatePoints(pointIds: Set<RoutePointId>, changes: PointChanges): Route {
        require(pointIds.isNotEmpty()) { "Select at least one point" }
        require(points.count { it.id in pointIds } == pointIds.size) { "Unknown point in selection" }
        val updated = points.map { point ->
            if (point.id in pointIds) changes.applyTo(point) else point
        }
        return Route(id, name, updated, RouteStatus.DRAFT, importedAtEpochMs, defaultDetectionRadius, scoringPattern)
    }

    fun renamePoint(pointId: RoutePointId, newName: String): Route =
        editPoint(pointId) { it.rename(newName) }

    fun changePointType(pointId: RoutePointId, newType: RoutePointType): Route =
        editPoint(pointId) { it.changeType(newType) }

    fun overridePointDetectionRadius(pointId: RoutePointId, radius: DetectionRadius?): Route =
        editPoint(pointId) { it.withDetectionRadiusOverride(radius) }

    fun assessReadiness(validator: RouteValidator = RouteValidator()): RouteReadiness {
        val validation = validator.validate(this)
        val ready = if (validation.canBeReady)
            Route(id, name, points, RouteStatus.READY, importedAtEpochMs, defaultDetectionRadius, scoringPattern) else null
        return RouteReadiness(validation, ready)
    }

    fun markReady(): Route = assessReadiness().readyRoute
        ?: throw IllegalArgumentException("A route with blocking issues cannot be ready")

    private fun editPoint(pointId: RoutePointId, edit: (RoutePoint) -> RoutePoint): Route {
        require(points.any { it.id == pointId }) { "Point does not belong to this route" }
        return Route(
            id, name,
            points.map { if (it.id == pointId) edit(it) else it },
            RouteStatus.DRAFT,
            importedAtEpochMs,
            defaultDetectionRadius,
            scoringPattern
        )
    }
}
