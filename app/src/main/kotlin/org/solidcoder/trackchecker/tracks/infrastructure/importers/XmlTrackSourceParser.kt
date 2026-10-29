package org.solidcoder.trackchecker.tracks.infrastructure.importers

import android.util.Xml
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.Locale
import org.solidcoder.trackchecker.tracks.application.ports.TrackImportSource
import org.solidcoder.trackchecker.tracks.application.ports.TrackSourceReader
import org.solidcoder.trackchecker.tracks.domain.model.GpsSample
import org.solidcoder.trackchecker.tracks.domain.model.TrackSegment
import org.xmlpull.v1.XmlPullParser

/** Reads ordered GPS geometry, unlike the official route point importer. */
class XmlTrackSourceParser(
    private val createParser: () -> XmlPullParser = { Xml.newPullParser() }
) : TrackSourceReader {
    override fun read(source: TrackImportSource): List<TrackSegment> {
        require(source.content.size <= MAX_BYTES) { "El track supera 20 MB" }
        val extension = source.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        require(extension == "gpx") { "Formato de track no admitido: .$extension" }
        val parser = createParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(ByteArrayInputStream(source.content), null)
        }
        parser.nextTag()
        require(parser.name.equals("gpx", true)) { "El archivo no contiene un GPX" }
        return parseGpx(parser).also { require(it.isNotEmpty()) { "El archivo no contiene un recorrido GPS" } }
    }

    private fun parseGpx(parser: XmlPullParser): List<TrackSegment> {
        val tracks = mutableListOf<TrackSegment>()
        val routes = mutableListOf<TrackSegment>()
        var segmentDepth = -1
        var routeDepth = -1
        var pointDepth = -1
        var currentSamples = mutableListOf<GpsSample>()
        var routeSamples = mutableListOf<GpsSample>()
        var pointName: String? = null
        var latitude: Double? = null
        var longitude: Double? = null
        var timestamp: Long? = null

        while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.DOCDECL -> error("El archivo XML contiene una declaración no permitida")
                XmlPullParser.START_TAG -> when {
                    parser.name == "trkseg" -> {
                        segmentDepth = parser.depth
                        currentSamples = mutableListOf()
                    }
                    parser.name == "rte" -> {
                        routeDepth = parser.depth
                        routeSamples = mutableListOf()
                    }
                    (parser.name == "trkpt" && segmentDepth > 0) ||
                        (parser.name == "rtept" && routeDepth > 0) -> {
                        pointDepth = parser.depth
                        pointName = parser.name
                        latitude = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        longitude = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        timestamp = null
                    }
                    pointDepth > 0 && parser.depth == pointDepth + 1 && parser.name == "time" -> {
                        timestamp = parseTimestamp(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> when {
                    parser.depth == pointDepth && parser.name == pointName -> {
                        val sample = GpsSample(
                            latitude ?: error("Latitud GPX inválida"),
                            longitude ?: error("Longitud GPX inválida"),
                            timestamp
                        )
                        if (pointName == "trkpt") currentSamples.add(sample) else routeSamples.add(sample)
                        pointDepth = -1
                    }
                    parser.name == "trkseg" && parser.depth == segmentDepth -> {
                        if (currentSamples.isNotEmpty()) tracks.add(TrackSegment(currentSamples))
                        segmentDepth = -1
                    }
                    parser.name == "rte" && parser.depth == routeDepth -> {
                        if (routeSamples.isNotEmpty()) routes.add(TrackSegment(routeSamples))
                        routeDepth = -1
                    }
                }
            }
        }
        return tracks.ifEmpty { routes }
    }

    private fun parseTimestamp(value: String): Long? =
        runCatching { Instant.parse(value.trim()).toEpochMilli() }.getOrNull()

    private companion object { const val MAX_BYTES = 20 * 1024 * 1024 }
}
