package org.solidcoder.trackchecker.scoring.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface EvaluationDao {
    @Transaction
    @Query("SELECT * FROM track_evaluations WHERE routeId = :routeId AND trackId = :trackId LIMIT 1")
    suspend fun find(routeId: String, trackId: String): EvaluationWithPoints?

    @Transaction
    @Query("SELECT * FROM track_evaluations WHERE routeId = :routeId ORDER BY evaluatedAtEpochMs DESC")
    suspend fun listForRoute(routeId: String): List<EvaluationWithPoints>

    @Upsert
    suspend fun upsert(record: EvaluationRecord)

    @Query("DELETE FROM evaluated_points WHERE evaluationId = :id")
    suspend fun deletePoints(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(records: List<EvaluatedPointRecord>)
}
