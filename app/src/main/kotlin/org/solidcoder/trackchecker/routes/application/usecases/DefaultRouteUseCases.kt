package org.solidcoder.trackchecker.routes.application.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.solidcoder.trackchecker.routes.application.ports.RouteCatalog
import org.solidcoder.trackchecker.routes.application.ports.RouteSourceReader
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.domain.repositories.RouteRepository
import org.solidcoder.trackchecker.routes.domain.services.RouteValidator

data class RouteUseCases(
    val observe: ObserveRoutesUseCase,
    val get: GetRouteUseCase,
    val import: ImportRouteUseCase,
    val rename: RenameRouteUseCase,
    val editPoint: EditRoutePointDetailsUseCase,
    val editPoints: EditRoutePointsUseCase,
    val setRadius: SetRouteDetectionRadiusUseCase,
    val applyPattern: ApplyRouteScoringPatternUseCase,
    val validate: ValidateRouteUseCase,
    val markReady: MarkRouteReadyUseCase,
    val delete: DeleteRouteUseCase
)

fun routeUseCases(
    repository: RouteRepository,
    catalog: RouteCatalog,
    reader: RouteSourceReader,
    validator: RouteValidator = RouteValidator(),
    clock: () -> Long = System::currentTimeMillis,
    parseDispatcher: CoroutineDispatcher = Dispatchers.Default
): RouteUseCases = RouteUseCases(
    observe = ObserveRoutesUseCase { catalog.observeRoutes() },
    get = GetRouteUseCase { repository.findById(it) },
    import = ImportRouteUseCase { source ->
        // XML decoding can be CPU-intensive; suspend alone does not switch dispatchers.
        val importedPoints = withContext(parseDispatcher) { reader.read(source) }
        require(importedPoints.isNotEmpty()) { "El archivo no contiene puntos de ruta" }
        val points = importedPoints.mapIndexed { index, item ->
            RoutePoint(
                id = RoutePointId.new(),
                name = item.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Punto ${index + 1}",
                coordinates = item.coordinates,
                type = item.type
            )
        }
        val routeName = source.fileName.substringBeforeLast('.').trim().ifBlank { "Nueva ruta" }
        val route = Route.imported(RouteId.new(), routeName, points, clock())
        repository.save(route)
        route
    },
    rename = RenameRouteUseCase { id, name ->
        val route = repository.findById(id) ?: error("Ruta no encontrada")
        repository.save(route.rename(name))
    },
    editPoint = EditRoutePointDetailsUseCase { details ->
        val route = repository.findById(details.routeId) ?: error("Ruta no encontrada")
        repository.save(route.renamePoint(details.pointId, details.name)
            .changePointType(details.pointId, details.type))
    },
    editPoints = EditRoutePointsUseCase { command ->
        val route = repository.findById(command.routeId) ?: error("Ruta no encontrada")
        repository.save(route.updatePoints(command.pointIds, command.changes))
    },
    setRadius = SetRouteDetectionRadiusUseCase { routeId, radius ->
        val route = repository.findById(routeId) ?: error("Ruta no encontrada")
        repository.save(route.changeDefaultDetectionRadius(radius))
    },
    applyPattern = ApplyRouteScoringPatternUseCase { command ->
        val route = repository.findById(command.routeId) ?: error("Ruta no encontrada")
        repository.save(route.applyScoringPattern(command.pattern, command.replaceOverrides))
    },
    validate = ValidateRouteUseCase { id ->
        val route = repository.findById(id) ?: error("Ruta no encontrada")
        validator.validate(route)
    },
    markReady = MarkRouteReadyUseCase { id ->
        val route = repository.findById(id) ?: error("Ruta no encontrada")
        val assessment = route.assessReadiness(validator)
        assessment.readyRoute?.let { repository.save(it) }
        assessment.validation
    },
    delete = DeleteRouteUseCase { repository.delete(it) }
)
