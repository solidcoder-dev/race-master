package org.picolobruno.racing.domain

data class RaceRoute(
    val start: Coordinate,
    val end: Coordinate,
    val waypoints: Collection<Waypoint>,
    val checkpoints: Collection<Checkpoint>
)
