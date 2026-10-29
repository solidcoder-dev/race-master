package org.solidcoder.trackchecker.routes.application

import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.solidcoder.trackchecker.routes.application.ports.*
import org.solidcoder.trackchecker.routes.application.usecases.EditRoutePointDetails
import org.solidcoder.trackchecker.routes.application.usecases.EditRoutePoints
import org.solidcoder.trackchecker.routes.application.usecases.ApplyRouteScoringPattern
import org.solidcoder.trackchecker.routes.application.usecases.routeUseCases
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.domain.repositories.RouteRepository

class RouteUseCasesTest {
    private class MemoryRoutes : RouteRepository, RouteCatalog {
        private val saved = linkedMapOf<RouteId, Route>()
        private val published = MutableStateFlow<List<Route>>(emptyList())
        fun size(): Int = saved.size
        override fun observeRoutes(): Flow<List<Route>> = published
        override suspend fun findById(id: RouteId): Route? = saved[id]
        override suspend fun save(route: Route) {
            saved[route.id] = route
            published.value = saved.values.toList()
        }
        override suspend fun delete(id: RouteId) {
            saved.remove(id)
            published.value = saved.values.toList()
        }
    }

    private val reader = RouteSourceReader {
        listOf(
            ImportedPoint("SALIDA", Coordinates(28.0, -15.0), RoutePointType.START),
            ImportedPoint("WP04", Coordinates(28.1, -15.1), RoutePointType.WAYPOINT),
            ImportedPoint("WP04", Coordinates(28.2, -15.2), RoutePointType.WAYPOINT),
            ImportedPoint("CP1", Coordinates(28.25, -15.25), RoutePointType.CHECKPOINT),
            ImportedPoint("META", Coordinates(28.3, -15.3), RoutePointType.FINISH)
        )
    }

    private fun source() = RouteImportSource("etapa.kml", byteArrayOf(1))

    @Test fun `importing the same file twice creates two independent routes and point identities`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, db, reader, clock = { 100L })
        val first = actions.import(source())
        val second = actions.import(source())
        assertNotEquals(first.id, second.id)
        assertNotEquals(first.points.first().id, second.points.first().id)
        assertEquals(2, db.size())
    }

    @Test fun `editing one named point leaves the other duplicate unchanged`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, db, reader)
        val route = actions.import(source())
        val id = route.points[1].id
        actions.editPoint(EditRoutePointDetails(route.id, id, "WP04 barranco", RoutePointType.WAYPOINT))
        val edited = checkNotNull(actions.get(route.id))
        assertEquals("WP04 barranco", edited.points[1].name)
        assertEquals("WP04", edited.points[2].name)
        assertEquals(id, edited.points[1].id)
    }

    @Test fun `bulk edits persist together and preserve the other point values`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, db, reader)
        val route = actions.import(source())
        actions.setRadius(route.id, DetectionRadius(75.0))
        actions.editPoints(EditRoutePoints(route.id, setOf(route.points[1].id, route.points[2].id),
            PointChanges(score = OverrideUpdate.Set(30), radius = OverrideUpdate.Set(DetectionRadius(90.0)))))
        val changed = checkNotNull(actions.get(route.id))
        assertEquals(75.0, changed.detectionRadiusFor(route.points[0].id).meters, 0.0)
        assertEquals(90.0, changed.detectionRadiusFor(route.points[1].id).meters, 0.0)
        assertEquals(30, changed.points[2].scoreValueOverride)
    }

    @Test fun `failed bulk edit persists none of its changes`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, db, reader)
        val route = actions.import(source())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                actions.editPoints(EditRoutePoints(route.id, setOf(route.points[1].id, route.points[2].id),
                    PointChanges(type = RoutePointType.START)))
            }
        }
        assertEquals(route.points, actions.get(route.id)?.points)
    }

    @Test fun `applying a pattern preserves personalized point values`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, db, reader)
        val route = actions.import(source())
        actions.editPoints(EditRoutePoints(route.id, setOf(route.points[1].id),
            PointChanges(score = OverrideUpdate.Set(25))))
        actions.applyPattern(ApplyRouteScoringPattern(route.id, ScoringPattern.MIXED))
        val changed = checkNotNull(actions.get(route.id))
        assertEquals(PointScoreEffect(whenPassed = 25), changed.scoringFor(route.points[1].id))
        assertEquals(PointScoreEffect(whenPassed = 10), changed.scoringFor(route.points[2].id))
    }

    @Test fun `validating reports issues without changing the route`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, db, reader)
        val route = actions.import(source())
        val validation = actions.validate(route.id)
        assertFalse(validation.canBeReady)
        assertEquals(RouteStatus.DRAFT, actions.get(route.id)?.status)
        assertEquals(RouteIssueCode.DUPLICATE_NAME, validation.issues.single().code)
    }

    @Test fun `marking a resolved route ready succeeds`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, db, reader)
        val route = actions.import(source())
        actions.editPoint(EditRoutePointDetails(route.id, route.points[2].id, "WP05", RoutePointType.WAYPOINT))
        val validation = actions.markReady(route.id)
        assertTrue(validation.canBeReady)
        assertEquals(RouteStatus.READY, actions.get(route.id)?.status)
        assertTrue(validation.issues.isEmpty())
    }

    @Test fun `an invalid route stays draft when confirmation is attempted`() = runBlocking {
        val db = MemoryRoutes()
        val onlyWaypoints = RouteSourceReader {
            listOf(ImportedPoint("WP1", Coordinates(28.0, -15.0), RoutePointType.WAYPOINT))
        }
        val actions = routeUseCases(db, db, onlyWaypoints)
        val route = actions.import(source())
        val validation = actions.markReady(route.id)
        assertFalse(validation.canBeReady)
        assertEquals(RouteStatus.DRAFT, actions.get(route.id)?.status)
    }

    @Test fun `deleting a route does not touch other imports`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, db, reader)
        val first = actions.import(source())
        val second = actions.import(source())
        actions.delete(first.id)
        assertNull(actions.get(first.id))
        assertNotNull(actions.get(second.id))
    }

    @Test fun `parsing runs on the configured worker dispatcher`() = runBlocking {
        val dispatcher = Executors.newSingleThreadExecutor { task ->
            Thread(task, "route-parser-worker")
        }.asCoroutineDispatcher()
        try {
            var parsingThread: String? = null
            val workerReader = RouteSourceReader {
                parsingThread = Thread.currentThread().name
                listOf(ImportedPoint("WP1", Coordinates(28.0, -15.0), RoutePointType.WAYPOINT))
            }
            val routes = MemoryRoutes()
            routeUseCases(routes, routes, workerReader, parseDispatcher = dispatcher).import(source())
            assertTrue(parsingThread?.startsWith("route-parser-worker") == true)
        } finally {
            dispatcher.close()
        }
    }
}
