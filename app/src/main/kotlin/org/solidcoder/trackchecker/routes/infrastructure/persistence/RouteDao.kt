package org.solidcoder.trackchecker.routes.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Transaction
    @Query("SELECT * FROM routes ORDER BY importedAtEpochMs DESC")
    fun observe(): Flow<List<RouteWithPoints>>

    @Transaction
    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): RouteWithPoints?

    @Upsert
    suspend fun upsertRoute(route: RouteRecord)

    @Upsert
    suspend fun upsertPoints(points: List<RoutePointRecord>)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRoute(id: String)
}
