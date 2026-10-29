package org.solidcoder.trackchecker.tracks.domain.model

import java.util.UUID
import org.solidcoder.trackchecker.routes.domain.model.RouteId

@JvmInline
value class TrackId(val value: String) {
    init { require(value.isNotBlank()) }
    companion object { fun new(): TrackId = TrackId(UUID.randomUUID().toString()) }
}

data class GpsSample(
    val latitude: Double,
    val longitude: Double,
    val timestampEpochMs: Long? = null
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

data class TrackSegment(val samples: List<GpsSample>) {
    init { require(samples.isNotEmpty()) { "Track segment cannot be empty" } }
}

/** A recorded path. Segment and sample order is preserved exactly as imported. */
class Track(
    val id: TrackId,
    val routeId: RouteId,
    val name: String,
    segments: List<TrackSegment>,
    val importedAtEpochMs: Long = 0L
) {
    val segments: List<TrackSegment> = segments.map { it.copy(samples = it.samples.toList()) }
    init {
        require(name.isNotBlank())
        require(this.segments.isNotEmpty()) { "Track must contain recorded positions" }
        require(importedAtEpochMs >= 0)
    }
    val sampleCount: Int get() = segments.sumOf { it.samples.size }
    val firstSample: GpsSample get() = segments.first().samples.first()
    val lastSample: GpsSample get() = segments.last().samples.last()
}

data class TrackSummary(
    val id: TrackId,
    val routeId: RouteId,
    val name: String,
    val importedAtEpochMs: Long
)
