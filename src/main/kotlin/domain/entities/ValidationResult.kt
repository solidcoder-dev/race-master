package domain.entities

data class ValidationResult(
    val isStartValid: Boolean,
    val isEndValid: Boolean,
    val reachedPoints: List<RoutePoint>,
    val missedPoints: List<RoutePoint>
)
