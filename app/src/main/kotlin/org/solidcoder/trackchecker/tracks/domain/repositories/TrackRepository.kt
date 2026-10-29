package org.solidcoder.trackchecker.tracks.domain.repositories

import org.solidcoder.trackchecker.tracks.domain.model.Track
import org.solidcoder.trackchecker.tracks.domain.model.TrackId

interface TrackRepository {
    suspend fun save(track: Track)
    suspend fun findById(id: TrackId): Track?
    suspend fun delete(id: TrackId)
}
