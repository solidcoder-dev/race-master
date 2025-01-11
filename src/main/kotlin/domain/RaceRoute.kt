package org.picolobruno.racing.domain

import org.picolobruno.racing.kml.domain.KmlDocument

data class RaceRoute(
    val name: String,
    val start: Coordinate,
    val end: Coordinate,
    val waypoints: Collection<Waypoint>,
    val checkpoints: Collection<Checkpoint>
) {
    companion object {
        fun from(kmlDocument: KmlDocument): RaceRoute {
            TODO()
        }
    }
}
