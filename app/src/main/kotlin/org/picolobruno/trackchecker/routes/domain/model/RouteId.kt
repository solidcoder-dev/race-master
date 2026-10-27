package org.picolobruno.trackchecker.routes.domain.model

import java.util.UUID

@JvmInline
value class RouteId(val value: String) {
    init { require(value.isNotBlank()) { "Route ID cannot be blank" } }
    companion object { fun new(): RouteId = RouteId(UUID.randomUUID().toString()) }
}

@JvmInline
value class RoutePointId(val value: String) {
    init { require(value.isNotBlank()) { "Route point ID cannot be blank" } }
    companion object { fun new(): RoutePointId = RoutePointId(UUID.randomUUID().toString()) }
}
