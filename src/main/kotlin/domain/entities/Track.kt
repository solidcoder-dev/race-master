package domain.entities

import java.util.*

class Track(trackPoints: List<TrackPoint>) {
    val points: TreeSet<TrackPoint> = TreeSet(trackPoints)

    fun isEmpty(): Boolean = points.isEmpty()
    fun firstPoint(): TrackPoint? = points.firstOrNull()
    fun lastPoint(): TrackPoint? = points.lastOrNull()
}
