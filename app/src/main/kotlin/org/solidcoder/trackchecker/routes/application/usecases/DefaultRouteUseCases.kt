package org.solidcoder.trackchecker.routes.application.usecases

import org.solidcoder.trackchecker.routes.application.ports.RouteSourceParser
import org.solidcoder.trackchecker.routes.domain.model.*
import org.solidcoder.trackchecker.routes.domain.repositories.RouteRepository
import org.solidcoder.trackchecker.routes.domain.services.RouteValidator

data class RouteUseCases(
    val observe: ObserveRoutesUseCase,
    val get: GetRouteUseCase,
    val import: ImportRouteUseCase,
    val rename: RenameRouteUseCase,
    val updatePoint: UpdateRoutePointUseCase,
    val validate: ValidateRouteUseCase,
    val delete: DeleteRouteUseCase
)

fun routeUseCases(
    repository: RouteRepository,
    parser: RouteSourceParser,
    validator: RouteValidator = RouteValidator(),
    clock: () -> Long = System::currentTimeMillis
): RouteUseCases = RouteUseCases(
    observe = ObserveRoutesUseCase { repository.observe() },
    get = GetRouteUseCase { repository.findById(it) },
    import = ImportRouteUseCase { fileName, content ->
        val importedPoints = parser.parse(fileName, content)
        require(importedPoints.isNotEmpty()) { "El archivo no contiene puntos de ruta" }
        val points = importedPoints.mapIndexed { index, item ->
            RoutePoint(
                id = RoutePointId.new(),
                name = item.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Punto ${index + 1}",
                coordinates = item.coordinates,
                type = item.type
            )
        }
        val routeName = fileName.substringBeforeLast('.').trim().ifBlank { "Nueva ruta" }
        val route = Route.imported(RouteId.new(), routeName, points, clock())
        repository.save(route)
        route
    },
    rename = RenameRouteUseCase { id, name ->
        val route = repository.findById(id) ?: error("Ruta no encontrada")
        repository.save(route.rename(name))
    },
    updatePoint = UpdateRoutePointUseCase { id, pointId, name, type ->
        val route = repository.findById(id) ?: error("Ruta no encontrada")
        repository.save(route.renamePoint(pointId, name).changePointType(pointId, type))
    },
    validate = ValidateRouteUseCase { id ->
        val route = repository.findById(id) ?: error("Ruta no encontrada")
        val result = validator.validate(route)
        if (result.canBeReady) repository.save(route.markReady(result))
        result
    },
    delete = DeleteRouteUseCase { repository.delete(it) }
)
