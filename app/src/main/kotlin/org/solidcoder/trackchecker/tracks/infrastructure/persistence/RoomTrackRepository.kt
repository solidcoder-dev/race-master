package org.solidcoder.trackchecker.tracks.infrastructure.persistence

import androidx.room.withTransaction
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoutesDatabase
import org.solidcoder.trackchecker.tracks.domain.model.GpsSample
import org.solidcoder.trackchecker.tracks.domain.model.Track
import org.solidcoder.trackchecker.tracks.domain.model.TrackId
import org.solidcoder.trackchecker.tracks.domain.model.TrackSegment
import org.solidcoder.trackchecker.tracks.domain.repositories.TrackRepository

class RoomTrackRepository(private val database: RoutesDatabase) : TrackRepository {
    private val dao = database.tracks()

    override suspend fun findById(id: TrackId): Track? = dao.findById(id.value)?.toDomain()

    override suspend fun save(track: Track) {
        database.withTransaction {
            dao.upsert(TrackRecord(track.id.value, track.name, track.importedAtEpochMs))
            dao.deleteSamples(track.id.value)
            dao.insertSamples(track.segments.flatMapIndexed { segmentIndex, segment ->
                segment.samples.mapIndexed { sampleIndex, sample ->
                    TrackSampleRecord(track.id.value, segmentIndex, sampleIndex,
                        sample.latitude, sample.longitude, sample.timestampEpochMs)
                }
            })
        }
    }

    override suspend fun delete(id: TrackId) = dao.delete(id.value)

    private fun TrackWithSamples.toDomain(): Track = Track(
        id = TrackId(track.id),
        name = track.name,
        importedAtEpochMs = track.importedAtEpochMs,
        segments = samples.groupBy { it.segmentIndex }.toSortedMap().values.map { records ->
            TrackSegment(records.sortedBy { it.sampleIndex }.map { record ->
                GpsSample(record.latitude, record.longitude, record.timestampEpochMs)
            })
        }
    )
}
