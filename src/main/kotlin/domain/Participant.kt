package org.picolobruno.racing.domain

import org.picolobruno.racing.kml.domain.KmlDocument

// TODO: this is not needed for now
data class Participant(
    val id: Long,
    val track: Track
) {
    companion object {
        fun from(kmlDocument: KmlDocument): Participant {
            TODO()
        }
    }
}
