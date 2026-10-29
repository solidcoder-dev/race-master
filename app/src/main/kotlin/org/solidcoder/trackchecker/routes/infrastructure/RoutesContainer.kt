package org.solidcoder.trackchecker.routes.infrastructure

import android.content.Context
import androidx.room.Room
import org.solidcoder.trackchecker.routes.application.usecases.RouteUseCases
import org.solidcoder.trackchecker.routes.application.usecases.routeUseCases
import org.solidcoder.trackchecker.routes.infrastructure.files.AndroidRouteFileReader
import org.solidcoder.trackchecker.routes.infrastructure.importers.XmlRouteSourceParser
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoomRouteRepository
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RoutesDatabase
import org.solidcoder.trackchecker.routes.infrastructure.persistence.MIGRATION_1_2
import org.solidcoder.trackchecker.routes.infrastructure.persistence.MIGRATION_2_3
import org.solidcoder.trackchecker.scoring.application.usecases.EvaluateTrackUseCase
import org.solidcoder.trackchecker.scoring.application.usecases.TrackCheckActions
import org.solidcoder.trackchecker.scoring.infrastructure.persistence.RoomEvaluationRepository
import org.solidcoder.trackchecker.tracks.application.usecases.ImportTrackUseCase
import org.solidcoder.trackchecker.tracks.infrastructure.importers.XmlTrackSourceParser
import org.solidcoder.trackchecker.tracks.infrastructure.persistence.RoomTrackRepository

/** Single-process composition root; the domain has no Android or Room dependencies. */
class RoutesContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        RoutesDatabase::class.java,
        "track-checker-routes.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

    private val repository = RoomRouteRepository(database)
    val fileReader = AndroidRouteFileReader(context.applicationContext.contentResolver)

    val useCases: RouteUseCases = routeUseCases(
        repository = repository,
        catalog = repository,
        reader = XmlRouteSourceParser()
    )

    private val tracks = RoomTrackRepository(database)
    private val evaluations = RoomEvaluationRepository(database)
    val trackChecks = TrackCheckActions(
        importTrack = ImportTrackUseCase(repository, tracks, XmlTrackSourceParser()),
        evaluate = EvaluateTrackUseCase(repository, tracks, evaluations),
        tracks = tracks,
        evaluations = evaluations
    )
}
