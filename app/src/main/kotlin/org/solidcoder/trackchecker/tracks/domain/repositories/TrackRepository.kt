package org.solidcoder.trackchecker.tracks.domain.repositories

import org.solidcoder.trackchecker.tracks.domain.model.Track
import org.solidcoder.trackchecker.tracks.domain.model.TrackId
import org.solidcoder.trackchecker.tracks.domain.model.TrackSummary
import org.solidcoder.trackchecker.routes.domain.model.RouteId

interface TrackRepository {
    suspend fun save(track: Track)
    suspend fun findById(id: TrackId): Track?
    suspend fun listForRoute(routeId: RouteId): List<TrackSummary>
    suspend fun delete(id: TrackId)
}
