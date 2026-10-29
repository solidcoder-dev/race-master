package org.solidcoder.trackchecker.tracks.application.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.solidcoder.trackchecker.routes.domain.model.RouteId
import org.solidcoder.trackchecker.routes.domain.model.RouteStatus
import org.solidcoder.trackchecker.routes.domain.repositories.RouteRepository
import org.solidcoder.trackchecker.routes.domain.services.RouteValidator
import org.solidcoder.trackchecker.tracks.application.ports.TrackImportSource
import org.solidcoder.trackchecker.tracks.application.ports.TrackSourceReader
import org.solidcoder.trackchecker.tracks.domain.model.Track
import org.solidcoder.trackchecker.tracks.domain.model.TrackId
import org.solidcoder.trackchecker.tracks.domain.repositories.TrackRepository

class ImportTrackUseCase(
    private val routes: RouteRepository,
    private val tracks: TrackRepository,
    private val reader: TrackSourceReader,
    private val validator: RouteValidator = RouteValidator(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> TrackId = TrackId::new,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(routeId: RouteId, source: TrackImportSource): Track {
        requireReady(routeId)
        val segments = withContext(parseDispatcher) { reader.read(source) }
        require(segments.isNotEmpty()) { "El archivo no contiene un recorrido GPS" }
        // Route status can change while a large file is being parsed.
        requireReady(routeId)
        val track = Track(
            id = newId(),
            routeId = routeId,
            name = source.fileName.substringBeforeLast('.').trim().ifBlank { "Nuevo track" },
            segments = segments,
            importedAtEpochMs = clock()
        )
        tracks.save(track)
        return track
    }

    private suspend fun requireReady(id: RouteId) {
        val route = routes.findById(id) ?: throw IllegalArgumentException("Ruta no encontrada")
        require(route.status == RouteStatus.READY && validator.validate(route).canBeReady) {
            "Valida la ruta antes de importar tracks"
        }
    }
}
