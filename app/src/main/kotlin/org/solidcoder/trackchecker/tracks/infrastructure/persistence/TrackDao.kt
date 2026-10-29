package org.solidcoder.trackchecker.tracks.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface TrackDao {
    @Transaction
    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TrackWithSamples?

    @Query("SELECT * FROM tracks WHERE routeId = :routeId ORDER BY importedAtEpochMs DESC")
    suspend fun listForRoute(routeId: String): List<TrackRecord>

    @Upsert
    suspend fun upsert(track: TrackRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<TrackSampleRecord>)

    @Query("DELETE FROM track_samples WHERE trackId = :trackId")
    suspend fun deleteSamples(trackId: String)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun delete(id: String)
}
