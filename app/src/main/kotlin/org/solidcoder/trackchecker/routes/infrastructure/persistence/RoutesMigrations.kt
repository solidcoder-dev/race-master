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
