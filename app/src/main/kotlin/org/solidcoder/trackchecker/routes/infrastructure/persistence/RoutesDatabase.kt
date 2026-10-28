package org.solidcoder.trackchecker.routes.infrastructure.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RouteRecord::class, RoutePointRecord::class],
    version = 1,
    exportSchema = true
)
abstract class RoutesDatabase : RoomDatabase() {
    abstract fun routes(): RouteDao
}
