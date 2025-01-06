package org.picolobruno.racing.domain

data class Track(
    val coordinates: Collection<Coordinate>
) {
    init {
        require(coordinates.isNotEmpty()) {
            "A track should shouldn't be empty!"
        }
    }
}
