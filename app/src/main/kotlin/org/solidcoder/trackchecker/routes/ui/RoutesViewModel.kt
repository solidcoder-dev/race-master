package org.solidcoder.trackchecker.routes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.solidcoder.trackchecker.routes.application.usecases.RouteUseCases
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource
import org.solidcoder.trackchecker.routes.application.usecases.EditRoutePointDetails

class RoutesViewModel(
    private val actions: RouteUseCases
) : ViewModel() {
    private val mutableRoutes = MutableStateFlow(RoutesUiState())
    val routes: StateFlow<RoutesUiState> = mutableRoutes

    private val mutableReview = MutableStateFlow<RouteReviewUiState>(RouteReviewUiState.Loading)
    val review: StateFlow<RouteReviewUiState> = mutableReview

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
            } catch (error: Exception) {
                mutableReview.value = RouteReviewUiState.Failed(
                    error.message ?: "No se pudo cargar la ruta"
                )
            }
        }
    }

    fun import(source: RouteImportSource, onImported: (RouteId) -> Unit) {
        mutableRoutes.update { it.copy(importing = true, error = null) }
        viewModelScope.launch {
            try {
                val route = actions.import(source)
                load(route.id)
                onImported(route.id)
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

    fun updatePoint(id: RouteId, pointId: RoutePointId, name: String, type: RoutePointType) =
        change(id) { actions.editPoint(EditRoutePointDetails(id, pointId, name, type)) }

    fun markReady(id: RouteId) = change(id) {
        actions.markReady(id)
    }

    fun delete(id: RouteId, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                actions.delete(id)
                mutableReview.value = RouteReviewUiState.NotFound
                onDeleted()
            } catch (error: Exception) {
                report(error)
            }
        }
    }

    fun report(error: Exception) {
        mutableRoutes.update { it.copy(error = error.message ?: "No se pudo completar la operación") }
    }

    fun clearError() {
        mutableRoutes.update { it.copy(error = null) }
    }

    private fun change(id: RouteId, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                load(id)
            } catch (error: Exception) {
                report(error)
            }
        }
    }

    private suspend fun load(id: RouteId) {
        val route = actions.get(id)
        mutableReview.value = if (route == null) {
            RouteReviewUiState.NotFound
        } else {
            RouteReviewUiState.Loaded(route, actions.validate(id).issues)
        }
    }

    companion object {
        fun factory(actions: RouteUseCases): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RoutesViewModel(actions) as T
            }
    }
}
