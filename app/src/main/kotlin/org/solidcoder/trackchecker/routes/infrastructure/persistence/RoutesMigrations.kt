package org.solidcoder.trackchecker.routes.infrastructure.persistence

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routes ADD COLUMN defaultDetectionRadiusMeters REAL NOT NULL DEFAULT 50.0")
        db.execSQL("ALTER TABLE routes ADD COLUMN scoringPattern TEXT")
        db.execSQL("ALTER TABLE route_points ADD COLUMN detectionRadiusOverrideMeters REAL")
        db.execSQL("ALTER TABLE route_points ADD COLUMN scoreValueOverride INTEGER")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tracks` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `importedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `track_samples` (`trackId` TEXT NOT NULL, `segmentIndex` INTEGER NOT NULL, `sampleIndex` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `timestampEpochMs` INTEGER, PRIMARY KEY(`trackId`, `segmentIndex`, `sampleIndex`), FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_track_samples_trackId` ON `track_samples` (`trackId`)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `track_evaluations` (`id` TEXT NOT NULL, `routeId` TEXT NOT NULL, `trackId` TEXT NOT NULL, `routeFingerprint` TEXT NOT NULL, `evaluatedAtEpochMs` INTEGER NOT NULL, `validStart` INTEGER NOT NULL, `validFinish` INTEGER NOT NULL, `scoreTotal` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`routeId`) REFERENCES `routes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_track_evaluations_routeId_trackId` ON `track_evaluations` (`routeId`, `trackId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_track_evaluations_trackId` ON `track_evaluations` (`trackId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `evaluated_points` (`evaluationId` TEXT NOT NULL, `pointId` TEXT NOT NULL, `status` TEXT NOT NULL, `minimumDistanceMeters` REAL NOT NULL, `detectionRadiusMeters` REAL NOT NULL, `scoreContribution` INTEGER, PRIMARY KEY(`evaluationId`, `pointId`), FOREIGN KEY(`evaluationId`) REFERENCES `track_evaluations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_evaluated_points_evaluationId` ON `evaluated_points` (`evaluationId`)")
    }
}
