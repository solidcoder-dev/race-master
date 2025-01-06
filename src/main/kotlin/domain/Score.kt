package org.picolobruno.racing.domain

data class Score(
    val points: Int
) {
    init {
        require (points >= 0) {
            "Score cannot be negative."
        }
    }

    operator fun plus(other: Score): Score =
        Score(this.points + other.points)
}
