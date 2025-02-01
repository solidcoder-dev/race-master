package org.picolobruno.racing.domain.participant

data class Track(
    val points: List<TrackPoint>,
    var score: Score = Score.Invalid("Non calculated yet.")
)

