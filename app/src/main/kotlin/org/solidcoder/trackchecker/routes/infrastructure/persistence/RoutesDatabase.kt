package org.solidcoder.trackchecker.routes.infrastructure.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import org.solidcoder.trackchecker.tracks.infrastructure.persistence.TrackDao
import org.solidcoder.trackchecker.tracks.infrastructure.persistence.TrackRecord
import org.solidcoder.trackchecker.tracks.infrastructure.persistence.TrackSampleRecord
import org.solidcoder.trackchecker.scoring.infrastructure.persistence.EvaluationDao
import org.solidcoder.trackchecker.scoring.infrastructure.persistence.EvaluationRecord
import org.solidcoder.trackchecker.scoring.infrastructure.persistence.EvaluatedPointRecord

@Database(
    entities = [RouteRecord::class, RoutePointRecord::class, TrackRecord::class, TrackSampleRecord::class,
        EvaluationRecord::class, EvaluatedPointRecord::class],
    version = 3,
    exportSchema = true
)
abstract class RoutesDatabase : RoomDatabase() {
    abstract fun routes(): RouteDao
    abstract fun tracks(): TrackDao
    abstract fun evaluations(): EvaluationDao
}
