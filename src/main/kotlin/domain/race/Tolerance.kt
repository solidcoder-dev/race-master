package org.picolobruno.racing.domain.race

data class Tolerance(
    val distanceTolerance: Double,
    val timeTolerance: Double
) {
    init {
        require(distanceTolerance >= 0) { "Distance tolerance must be non-negative." }
        require(timeTolerance >= 0) { "Time tolerance must be non-negative." }
    }
}
