package org.picolobruno.racing.kml.domain

data class KmlDocument(
    val name: String,
    val placemarks: List<Placemark>
)