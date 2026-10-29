package org.solidcoder.trackchecker.routes.infrastructure

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.solidcoder.trackchecker.routes.infrastructure.persistence.MIGRATION_1_2
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoutesDatabase

@RunWith(AndroidJUnit4::class)
class RoutesMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), RoutesDatabase::class.java
    )

    @Test fun migrationPreservesImportedPointsAndAddsRuleDefaults() {
        val name = "route-migration-test"
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO routes (id, name, status, importedAtEpochMs) VALUES ('route', 'Etapa', 'DRAFT', 100)")
            execSQL("""
                INSERT INTO route_points (id, routeId, sequence, name, latitude, longitude, type)
                VALUES ('wp', 'route', 0, 'WP01', 28.0, -15.0, 'WAYPOINT')
            """.trimIndent())
            close()
        }

        helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).use { database ->
            database.query("SELECT defaultDetectionRadiusMeters, scoringPattern FROM routes WHERE id = 'route'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(50.0, cursor.getDouble(0), 0.0)
                    assertTrue(cursor.isNull(1))
                }
            database.query("SELECT name, type, detectionRadiusOverrideMeters, scoreValueOverride FROM route_points WHERE id = 'wp'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("WP01", cursor.getString(0))
                    assertEquals("WAYPOINT", cursor.getString(1))
                    assertTrue(cursor.isNull(2))
                    assertTrue(cursor.isNull(3))
                }
        }
    }
}
