package org.solidcoder.trackchecker.tracks.application.ports

import org.solidcoder.trackchecker.tracks.domain.model.TrackSegment

/** File bytes are supplied by the infrastructure, never Android URIs in the domain. */
data class TrackImportSource(val fileName: String, val content: ByteArray)

fun interface TrackSourceReader {
    fun read(source: TrackImportSource): List<TrackSegment>
}
