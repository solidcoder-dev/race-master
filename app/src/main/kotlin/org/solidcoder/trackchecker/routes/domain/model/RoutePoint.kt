package org.solidcoder.trackchecker.routes.domain.model

enum class RoutePointType {
    START, FINISH, WAYPOINT, ECLIPSE_WAYPOINT, CHECKPOINT, OTHER
}

data class RoutePoint(
    val id: RoutePointId,
    val name: String,
    val coordinates: Coordinates,
    val type: RoutePointType,
    val detectionRadiusOverride: DetectionRadius? = null,
    val scoreValueOverride: Int? = null
) {
    init {
        require(name.isNotBlank()) { "Point name cannot be blank" }
        require(scoreValueOverride == null || scoreValueOverride >= 0) { "Score must be non-negative" }
    }

    fun rename(newName: String): RoutePoint {
        require(newName.isNotBlank()) { "Point name cannot be blank" }
        return copy(name = newName.trim())
    }

    fun changeType(newType: RoutePointType): RoutePoint = copy(type = newType)

    fun withDetectionRadiusOverride(radius: DetectionRadius?): RoutePoint =
        copy(detectionRadiusOverride = radius)

    fun withScoreValueOverride(value: Int?): RoutePoint = copy(scoreValueOverride = value)
}
