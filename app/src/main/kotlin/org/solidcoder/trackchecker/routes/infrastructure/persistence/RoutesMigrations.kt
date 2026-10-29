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
