package org.picolobruno.trackchecker.routes.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoute(route: RouteRecord)

    @Query("DELETE FROM route_points WHERE routeId = :routeId")
    suspend fun removePoints(routeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<RoutePointRecord>)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRoute(id: String)
}
