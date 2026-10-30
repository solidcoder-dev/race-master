package org.solidcoder.trackchecker.tracks.infrastructure.importers

import android.util.Xml
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Locale
import java.util.zip.ZipInputStream
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
        val bytes = when (extension) {
            "gpx", "kml" -> source.content
            "kmz" -> unzipKml(source.content)
            else -> error("Formato de track no admitido: .$extension")
        }
        val isKml = extension == "kml" || extension == "kmz"
        val parser = createParser().configure(bytes, processNamespaces = !isKml)
        parser.nextTag()
        val result = when (localName(parser.name).lowercase(Locale.ROOT)) {
            "gpx" -> parseGpx(parser)
            "kml" -> parseKml(parser)
            else -> error("El archivo no contiene un KML o GPX")
        }
        return result.also { require(it.isNotEmpty()) { "El archivo no contiene un recorrido GPS" } }
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

    private fun parseKml(parser: XmlPullParser): List<TrackSegment> {
        val segments = mutableListOf<TrackSegment>()
        var lineDepth = -1
        var trackDepth = -1
        var coordinates = mutableListOf<GpsSample>()
        var timestamps = mutableListOf<Long?>()
        while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.DOCDECL -> error("El archivo XML contiene una declaración no permitida")
                XmlPullParser.START_TAG -> when {
                    isCoreKmlTag(parser.name, "LineString") -> lineDepth = parser.depth
                    isGoogleExtensionTag(parser.name, "Track") -> {
                        trackDepth = parser.depth
                        coordinates = mutableListOf()
                        timestamps = mutableListOf()
                    }
                    lineDepth > 0 && parser.depth == lineDepth + 1 && isCoreKmlTag(parser.name, "coordinates") -> {
                        val points = parser.nextText().trim().split(Regex("\\s+"))
                            .filter { it.isNotBlank() }
                            .map(::parseKmlCoordinate)
                        if (points.isNotEmpty()) segments.add(TrackSegment(points))
                    }
                    trackDepth > 0 && parser.depth == trackDepth + 1 && isGoogleExtensionTag(parser.name, "coord") -> {
                        val values = parser.nextText().trim().split(Regex("\\s+"))
                        require(values.size >= 2) { "Coordenada gx:Track incompleta" }
                        coordinates.add(GpsSample(values[1].toDouble(), values[0].toDouble()))
                    }
                    trackDepth > 0 && parser.depth == trackDepth + 1 && localName(parser.name) == "when" -> {
                        timestamps.add(parseTimestamp(parser.nextText()))
                    }
                }
                XmlPullParser.END_TAG -> when {
                    isCoreKmlTag(parser.name, "LineString") && parser.depth == lineDepth -> lineDepth = -1
                    isGoogleExtensionTag(parser.name, "Track") && parser.depth == trackDepth -> {
                        if (coordinates.isNotEmpty()) {
                            val samples = coordinates.mapIndexed { index, point ->
                                point.copy(timestampEpochMs = timestamps.getOrNull(index))
                            }
                            segments.add(TrackSegment(samples))
                        }
                        trackDepth = -1
                    }
                }
            }
        }
        return segments
    }

    private fun parseKmlCoordinate(raw: String): GpsSample {
        val parts = raw.split(',')
        require(parts.size >= 2) { "Coordenada KML incompleta" }
        return GpsSample(parts[1].toDouble(), parts[0].toDouble())
    }

    private fun unzipKml(content: ByteArray): ByteArray {
        ZipInputStream(ByteArrayInputStream(content)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name.endsWith(".kml", ignoreCase = true)) {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        require(output.size() + count <= MAX_BYTES) { "KML comprimido demasiado grande" }
                        output.write(buffer, 0, count)
                    }
                    return output.toByteArray()
                }
                zip.closeEntry()
            }
            error("KMZ sin archivo KML")
        }
    }

    private fun localName(name: String?): String = name?.substringAfter(':').orEmpty()

    private fun isCoreKmlTag(name: String?, expected: String): Boolean = name == expected

    private fun isGoogleExtensionTag(name: String?, expected: String): Boolean = name == "gx:$expected"

    private fun XmlPullParser.configure(bytes: ByteArray, processNamespaces: Boolean): XmlPullParser = apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, processNamespaces)
        setInput(ByteArrayInputStream(bytes), null)
    }

    private companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
    }
}
