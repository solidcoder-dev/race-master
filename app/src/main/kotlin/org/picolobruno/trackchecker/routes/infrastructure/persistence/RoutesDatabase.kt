package org.picolobruno.trackchecker.routes.infrastructure.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RouteRecord::class, RoutePointRecord::class],
    version = 1,
    exportSchema = false
)
abstract class RoutesDatabase : RoomDatabase() {
    abstract fun routes(): RouteDao
}
