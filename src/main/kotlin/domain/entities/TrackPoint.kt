package domain.entities

data class TrackPoint(val timestamp: Long, override val latitude: Double, override val longitude: Double) :
    Point(latitude, longitude), Comparable<TrackPoint> {

    override fun compareTo(other: TrackPoint): Int = this.timestamp.compareTo(other.timestamp)
}
