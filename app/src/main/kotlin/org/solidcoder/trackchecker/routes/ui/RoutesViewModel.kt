package org.solidcoder.trackchecker.routes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.solidcoder.trackchecker.routes.application.usecases.RouteUseCases
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource
import org.solidcoder.trackchecker.routes.application.usecases.EditRoutePointDetails
import org.solidcoder.trackchecker.routes.application.usecases.EditRoutePoints
import org.solidcoder.trackchecker.routes.application.usecases.ApplyRouteScoringPattern
import org.solidcoder.trackchecker.scoring.application.usecases.TrackCheckActions
import org.solidcoder.trackchecker.tracks.application.ports.TrackImportSource
import org.solidcoder.trackchecker.tracks.application.importSequentially
import org.solidcoder.trackchecker.tracks.domain.model.TrackId
import org.xml.sax.SAXException
import org.xmlpull.v1.XmlPullParserException

class RoutesViewModel(
    private val actions: RouteUseCases,
    private val trackChecks: TrackCheckActions
) : ViewModel() {
    private val mutableRoutes = MutableStateFlow(RoutesUiState())
    val routes: StateFlow<RoutesUiState> = mutableRoutes

    private val mutableReview = MutableStateFlow<RouteReviewUiState>(RouteReviewUiState.Loading)
    val review: StateFlow<RouteReviewUiState> = mutableReview

    private val mutableEvents = Channel<RoutesUiEvent>(Channel.BUFFERED)
    val events = mutableEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            actions.observe().collect { routes ->
                mutableRoutes.update { it.copy(routes = routes) }
            }
        }
    }

    fun open(id: RouteId) {
        mutableReview.value = RouteReviewUiState.Loading
        viewModelScope.launch {
            try {
                load(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableReview.value = RouteReviewUiState.Failed(
                    error.message ?: "No se pudo cargar la ruta"
                )
            }
        }
    }

    fun import(source: RouteImportSource, onImported: (RouteId) -> Unit) {
        mutableRoutes.update { it.copy(importing = true) }
        viewModelScope.launch {
            try {
                val route = actions.import(source)
                load(route.id)
                onImported(route.id)
                mutableEvents.send(RoutesUiEvent.Message("Ruta importada correctamente."))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                report(error)
            } finally {
                mutableRoutes.update { it.copy(importing = false) }
            }
        }
    }

    fun rename(id: RouteId, name: String) = change(id) {
        actions.rename(id, name)
    }

    fun updatePoint(id: RouteId, pointId: RoutePointId, name: String, type: RoutePointType, changes: PointChanges?) =
        change(id) { actions.editPoint(EditRoutePointDetails(id, pointId, name, type, changes)) }

    fun updatePoints(id: RouteId, pointIds: Set<RoutePointId>, changes: PointChanges) =
        change(id) { actions.editPoints(EditRoutePoints(id, pointIds, changes)) }

    fun setDetectionRadius(id: RouteId, radius: DetectionRadius) =
        change(id) { actions.setRadius(id, radius) }

    fun applyPattern(id: RouteId, pattern: ScoringPattern, replaceOverrides: Boolean) =
        change(id) { actions.applyPattern(ApplyRouteScoringPattern(id, pattern, replaceOverrides)) }

    fun markReady(id: RouteId) = change(id) {
        actions.markReady(id)
    }

    fun selectTrack(id: TrackId?) {
        val loaded = mutableReview.value as? RouteReviewUiState.Loaded ?: return
        if (id != null && loaded.tracks.none { it.track.id == id }) return
        mutableReview.value = loaded.copy(
            selectedTrackId = id,
            selectedTrack = loaded.selectedTrack?.takeIf { it.id == id }
        )
        if (id != null && loaded.selectedTrack?.id != id) {
            viewModelScope.launch {
                try {
                    val selected = trackChecks.load(loaded.route.id, id)
                    mutableReview.update { current ->
                        if (current is RouteReviewUiState.Loaded &&
                            current.route.id == loaded.route.id && current.selectedTrackId == id
                        ) current.copy(selectedTrack = selected) else current
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    report(error)
                }
            }
        }
    }

    fun importTracks(routeId: RouteId, sources: List<suspend () -> TrackImportSource>) {
        mutableReview.update { state ->
            if (state is RouteReviewUiState.Loaded) state.copy(
                importingTrack = true,
                trackImportProgress = TrackImportProgress(0, sources.size)
            ) else state
        }
        viewModelScope.launch {
            try {
                val result = importSequentially(
                    sources = sources,
                    onProgress = { current, total ->
                        mutableReview.update { state ->
                            if (state is RouteReviewUiState.Loaded) state.copy(
                                importingTrack = true,
                                trackImportProgress = TrackImportProgress(current, total)
                            ) else state
                        }
                    },
                    process = { source -> trackChecks.importAndEvaluate(routeId, source) }
                )
                load(routeId)
                mutableEvents.send(RoutesUiEvent.Message(
                    "Importados ${result.succeeded} de ${sources.size}. Fallos: ${result.failed}."
                ))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                report(error)
            } finally {
                mutableReview.update { state ->
                    if (state is RouteReviewUiState.Loaded) state.copy(
                        importingTrack = false,
                        trackImportProgress = null
                    ) else state
                }
            }
        }
    }

    fun recalculateTrack(routeId: RouteId, trackId: TrackId) = change(routeId) {
        trackChecks.recalculate(routeId, trackId)
    }

    fun deleteTrack(routeId: RouteId, trackId: TrackId) = change(routeId) {
        trackChecks.delete(routeId, trackId)
    }

    fun delete(id: RouteId, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                actions.delete(id)
                mutableReview.value = RouteReviewUiState.NotFound
                onDeleted()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                report(error)
            }
        }
    }

    fun report(error: Exception) {
        mutableRoutes.update { it.copy(importing = false) }
        mutableEvents.trySend(RoutesUiEvent.Message(errorMessage(error), isError = true))
    }

    fun beginImport() {
        mutableRoutes.update { it.copy(importing = true) }
    }

    fun cancelImport() {
        mutableRoutes.update { it.copy(importing = false) }
        mutableEvents.trySend(RoutesUiEvent.Message("No se seleccionó ningún archivo. Puedes intentarlo de nuevo."))
    }

    private fun change(id: RouteId, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                load(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                report(error)
            }
        }
    }

    private suspend fun load(id: RouteId) {
        val route = actions.get(id)
        val previous = mutableReview.value as? RouteReviewUiState.Loaded
        mutableReview.value = if (route == null) {
            RouteReviewUiState.NotFound
        } else {
            val tracks = trackChecks.list(id)
            val selected = previous?.selectedTrackId?.takeIf { selectedId -> tracks.any { it.track.id == selectedId } }
            val geometry = selected?.let { trackId ->
                previous?.selectedTrack?.takeIf { it.id == trackId && it.routeId == id }
                    ?: trackChecks.load(id, trackId)
            }
            RouteReviewUiState.Loaded(route, actions.validate(id).issues, tracks, selected, geometry)
        }
    }

    private fun errorMessage(error: Exception): String = when (error) {
        is SAXException, is XmlPullParserException -> "No se pudo interpretar el archivo. Comprueba que sea un KML, GPX o KMZ válido."
        is SecurityException, is IOException -> "No se pudo acceder al archivo. Vuelve a seleccionarlo e inténtalo de nuevo."
        else -> error.message
            ?.takeIf { it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://") }
            ?: "No se pudo completar la operación. Inténtalo de nuevo."
    }

    companion object {
        fun factory(actions: RouteUseCases, trackChecks: TrackCheckActions): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RoutesViewModel(actions, trackChecks) as T
            }
    }
}
