package org.picolobruno.racing.domain

data class Tolerance(
    val distanceInKm: Double
) {
    init {
        require(distanceInKm >= 0) {
            "Tolerance cannot be negative."
        }
    }
}
