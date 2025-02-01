package org.picolobruno.racing.domain.race

class Race(
    val id: String,
    val name: String,
    var tolerance: Tolerance,
    val scoringRules: Map<String, Double> = emptyMap(),
    private val racepoints: Set<Racepoint>
) {

    init {
        require(waypoints.isNotEmpty()) {
            "A race should have at least 1 waypoint."
        }

        require(checkpoints.isNotEmpty()) {
            "A race should have at least 1 checkpoint."
        }
    }

    val startPoint: Racepoint // TODO: add invariant to check that this isn't null
        get() = racepoints.first {
            it.name.contains("START")
        }

    val endpoint: Racepoint
        get() = racepoints.first {
            it.name.contains("END")
        }

    val waypoints: List<Racepoint>
        get() = racepoints.filter {
            it.name.contains("WP")
        }

    val checkpoints: List<Racepoint>
        get() = racepoints.filter {
            it.name.contains("CP")
        }
}
