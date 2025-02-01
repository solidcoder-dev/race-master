package org.picolobruno.racing.domain.participant

import org.picolobruno.racing.domain.race.Racepoint


sealed class Score {
    data class Invalid (
        val message: String
    ) : Score()

    data class Defined(
        val successes: Set<Pair<Double, Racepoint>> = emptySet(),
        val penalties: Set<Pair<Double, Racepoint>> = emptySet()
    ): Score() {
        val total
            get() = successes.sumOf { it.first } + penalties.sumOf { it.first }

        fun addSuccess(value: Double, waypoint: Racepoint) {
            successes.plus(Pair(value, waypoint))
        }

        fun addPenalty(value: Double, checkpoint: Racepoint) {
            penalties.plus(Pair(value, checkpoint))
        }
    }
}
