package org.solidcoder.trackchecker.routes.infrastructure

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.routes.domain.model.Route
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RoutePoint
import org.solidcoder.trackchecker.routes.domain.model.RoutePointId
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.solidcoder.trackchecker.routes.domain.model.DetectionRadius
import org.solidcoder.trackchecker.routes.domain.model.ScoringPattern
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoomRouteRepository
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoutesDatabase

@RunWith(AndroidJUnit4::class)
class RoomRouteRepositoryTest {
    private lateinit var database: RoutesDatabase
    private lateinit var repository: RoomRouteRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RoutesDatabase::class.java).build()
        repository = RoomRouteRepository(database)
    }

    @After fun tearDown() {
        database.close()
    }

    @Test fun savingAndRestoringPreservesAllPointIdentities() = runBlocking {
        val original = route("one")
        repository.save(original)

        val restored = requireNotNull(repository.findById(original.id))
        assertEquals(original.name, restored.name)
        assertEquals(original.points, restored.points)
    }

    @Test fun savingAndRestoringPreservesScoringPatternAndRadiusOverrides() = runBlocking {
        val original = route("configured")
            .changeDefaultDetectionRadius(DetectionRadius(75.0))
            .overridePointDetectionRadius(RoutePointId("wp1-configured"), DetectionRadius(120.0))
            .overridePointScore(RoutePointId("wp1-configured"), 25)
            .applyScoringPattern(ScoringPattern.MIXED)
        repository.save(original)

        val restored = requireNotNull(repository.findById(original.id))
        assertEquals(original.defaultDetectionRadius, restored.defaultDetectionRadius)
        assertEquals(original.scoringPattern, restored.scoringPattern)
        assertEquals(original.points, restored.points)
    }

    @Test fun renamingDoesNotCascadeDeleteOtherPointReferences() = runBlocking {
        val original = route("one")
        repository.save(original)
        val linkedPointId = original.points[1].id.value
        val sqlite = database.openHelper.writableDatabase
        sqlite.execSQL(
            "CREATE TABLE point_links (id INTEGER PRIMARY KEY, pointId TEXT NOT NULL REFERENCES route_points(id) ON DELETE CASCADE)"
        )
        sqlite.execSQL("INSERT INTO point_links (id, pointId) VALUES (?, ?)", arrayOf<Any?>(1, linkedPointId))

        repository.save(original.rename("Nueva etapa"))
        repository.save(original.renamePoint(original.points[1].id, "WP04 actualizado"))

        sqlite.query("SELECT COUNT(*) FROM point_links").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        val restored = requireNotNull(repository.findById(original.id))
        assertEquals("WP04 actualizado", restored.points[1].name)
        assertEquals(original.points[1].id, restored.points[1].id)
        assertEquals("WP04", restored.points[2].name)
    }

    @Test fun deletingOneRouteDoesNotDeleteOtherImports() = runBlocking {
        val first = route("one")
        val second = route("two")
        repository.save(first)
        repository.save(second)

        repository.delete(first.id)

        assertNull(repository.findById(first.id))
        assertNotNull(repository.findById(second.id))
        assertEquals(second.points, repository.findById(second.id)?.points)
    }

    private fun route(id: String): Route = Route.imported(
        id = RouteId(id),
        name = "Etapa $id",
        points = listOf(
            RoutePoint(RoutePointId("start-$id"), "SALIDA", Coordinates(28.0, -15.0), RoutePointType.START),
            RoutePoint(RoutePointId("wp1-$id"), "WP04", Coordinates(28.1, -15.1), RoutePointType.WAYPOINT),
            RoutePoint(RoutePointId("wp2-$id"), "WP04", Coordinates(28.2, -15.2), RoutePointType.WAYPOINT),
            RoutePoint(RoutePointId("finish-$id"), "META", Coordinates(28.3, -15.3), RoutePointType.FINISH)
        ),
        importedAtEpochMs = 100L
    )
}
