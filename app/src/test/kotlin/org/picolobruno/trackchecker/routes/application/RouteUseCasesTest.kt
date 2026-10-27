package org.picolobruno.trackchecker.routes.application

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.picolobruno.trackchecker.routes.application.ports.ImportedPoint
import org.picolobruno.trackchecker.routes.application.ports.RouteSourceParser
import org.picolobruno.trackchecker.routes.application.usecases.routeUseCases
import org.picolobruno.trackchecker.routes.domain.model.*
import org.picolobruno.trackchecker.routes.domain.repositories.RouteRepository

class RouteUseCasesTest {
    private class MemoryRoutes : RouteRepository {
        private val saved = linkedMapOf<RouteId, Route>()
        private val published = MutableStateFlow<List<Route>>(emptyList())
        fun size(): Int = saved.size
        override fun observe(): Flow<List<Route>> = published
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

    private val parser = object : RouteSourceParser {
        override fun parse(fileName: String, content: ByteArray) = listOf(
            ImportedPoint("SALIDA", Coordinates(28.0, -15.0), RoutePointType.START),
            ImportedPoint("WP04", Coordinates(28.1, -15.1), RoutePointType.WAYPOINT),
            ImportedPoint("WP04", Coordinates(28.2, -15.2), RoutePointType.WAYPOINT),
            ImportedPoint("META", Coordinates(28.3, -15.3), RoutePointType.FINISH)
        )
    }

    @Test fun `importing the same file twice creates two independent routes and point identities`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, parser, clock = { 100L })
        val first = actions.import("etapa.kml", byteArrayOf(1))
        val second = actions.import("etapa.kml", byteArrayOf(1))
        assertNotEquals(first.id, second.id)
        assertNotEquals(first.points.first().id, second.points.first().id)
        assertEquals(2, db.size())
    }

    @Test fun `editing one named point leaves the other duplicate unchanged`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, parser)
        val route = actions.import("etapa.kml", byteArrayOf(1))
        val id = route.points[1].id
        actions.updatePoint(route.id, id, "WP04 barranco", RoutePointType.WAYPOINT)
        val edited = checkNotNull(actions.get(route.id))
        assertEquals("WP04 barranco", edited.points[1].name)
        assertEquals("WP04", edited.points[2].name)
        assertEquals(id, edited.points[1].id)
    }

    @Test fun `duplicate names remain a warning and can mark route ready`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, parser)
        val route = actions.import("etapa.kml", byteArrayOf(1))
        val validation = actions.validate(route.id)
        assertTrue(validation.canBeReady)
        assertEquals(RouteStatus.READY, actions.get(route.id)?.status)
        assertEquals(RouteIssueCode.DUPLICATE_NAME, validation.issues.single().code)
    }

    @Test fun `deleting a route does not touch other imports`() = runBlocking {
        val db = MemoryRoutes()
        val actions = routeUseCases(db, parser)
        val first = actions.import("etapa.kml", byteArrayOf(1))
        val second = actions.import("etapa.kml", byteArrayOf(1))
        actions.delete(first.id)
        assertNull(actions.get(first.id))
        assertNotNull(actions.get(second.id))
    }
}
