package domain.entities

data class RoutePoint(val name: String, override val latitude: Double, override val longitude: Double) :
    Point(latitude, longitude)
