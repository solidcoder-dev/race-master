package org.picolobruno.racing.domain.participant

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long // e.g., epoch time in milliseconds
)

