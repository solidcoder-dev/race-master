package org.solidcoder.trackchecker.routes.infrastructure

import android.content.Context
import androidx.room.Room
import org.solidcoder.trackchecker.routes.application.usecases.RouteUseCases
import org.solidcoder.trackchecker.routes.application.usecases.routeUseCases
import org.solidcoder.trackchecker.routes.infrastructure.importers.XmlRouteSourceParser
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoomRouteRepository
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoutesDatabase

/** Single-process composition root. Nothing leaks into the pure domain model. */
class RoutesContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        RoutesDatabase::class.java,
        "track-checker-routes.db"
    ).build()

    val useCases: RouteUseCases = routeUseCases(
        repository = RoomRouteRepository(database),
        parser = XmlRouteSourceParser()
    )
}
