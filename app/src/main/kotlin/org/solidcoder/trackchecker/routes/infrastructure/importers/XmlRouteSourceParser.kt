package org.solidcoder.trackchecker.routes.infrastructure.importers

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import android.util.Xml
import org.solidcoder.trackchecker.routes.application.ports.ImportedPoint
import org.solidcoder.trackchecker.routes.application.ports.RouteSourceReader
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource
import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.xmlpull.v1.XmlPullParser

/**
 * Imports route points only. Track geometries are intentionally ignored.
 * Parsing of source XML happens behind the application port, never in domain.
 */
class XmlRouteSourceParser : RouteSourceReader {
    override fun read(source: RouteImportSource): List<ImportedPoint> {
        val fileName = source.fileName
        val content = source.content
        require(content.size <= MAX_FILE_BYTES) { "El archivo supera 10 MB" }
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val bytes = when (extension) {
            "kml", "gpx" -> content
            "kmz" -> unzipKml(content)
            else -> throw IllegalArgumentException("Formato no admitido: .$extension")
        }
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            // Android's pull parser ignores DTD declarations unless processing is enabled.
            setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
            setInput(ByteArrayInputStream(bytes), null)
        }
        parser.nextTag()
        return when (parser.name.lowercase(Locale.ROOT)) {
            "kml" -> parseKml(parser)
            "gpx" -> parseGpx(parser)
            else -> throw IllegalArgumentException("El archivo no contiene un documento KML o GPX")
        }
    }

    private fun parseKml(parser: XmlPullParser): List<ImportedPoint> {
        val points = mutableListOf<ImportedPoint>()
        var placemarkDepth = -1
        var pointDepth = -1
        var name: String? = null
        var coordinates: String? = null

        while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.DOCDECL -> throw IllegalArgumentException("El archivo XML contiene una declaración no permitida")
                XmlPullParser.START_TAG -> when {
                    parser.name == "Placemark" -> {
                        placemarkDepth = parser.depth
                        pointDepth = -1
                        name = null
                        coordinates = null
                    }
                    placemarkDepth > 0 && parser.depth == placemarkDepth + 1 && parser.name == "name" -> {
                        name = parser.nextText().trim().takeIf { it.isNotEmpty() }
                    }
                    placemarkDepth > 0 && parser.depth == placemarkDepth + 1 && parser.name == "Point" -> {
                        pointDepth = parser.depth
                    }
                    pointDepth > 0 && parser.depth == pointDepth + 1 && parser.name == "coordinates" -> {
                        coordinates = parser.nextText().trim()
                    }
                }
                XmlPullParser.END_TAG -> if (
                    parser.name == "Placemark" && parser.depth == placemarkDepth
                ) {
                    // Only a direct Point geometry belongs to an official route waypoint.
                    coordinates?.let { points += parseKmlPoint(name, it) }
                    placemarkDepth = -1
                    pointDepth = -1
                }
            }
        }
        return points
    }

    private fun parseKmlPoint(name: String?, rawCoordinates: String): ImportedPoint {
        val components = rawCoordinates.split(Regex("[,\\s]+"))
        require(components.size >= 2) { "Coordenadas KML incompletas" }
        val longitude = components[0].toDoubleOrNull() ?: error("Longitud KML inválida")
        val latitude = components[1].toDoubleOrNull() ?: error("Latitud KML inválida")
        return ImportedPoint(name, Coordinates(latitude, longitude), classify(name))
    }

    private fun parseGpx(parser: XmlPullParser): List<ImportedPoint> {
        val points = mutableListOf<ImportedPoint>()
        var pointDepth = -1
        var latitude: Double? = null
        var longitude: Double? = null
        var name: String? = null

        while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.DOCDECL -> throw IllegalArgumentException("El archivo XML contiene una declaración no permitida")
                XmlPullParser.START_TAG -> when {
                    parser.name == "wpt" || parser.name == "rtept" -> {
                        pointDepth = parser.depth
                        latitude = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        longitude = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        name = null
                    }
                    pointDepth > 0 && parser.depth == pointDepth + 1 && parser.name == "name" -> {
                        name = parser.nextText().trim().takeIf { it.isNotEmpty() }
                    }
                }
                XmlPullParser.END_TAG -> if (
                    (parser.name == "wpt" || parser.name == "rtept") && parser.depth == pointDepth
                ) {
                    val pointLatitude = latitude ?: error("Latitud GPX inválida")
                    val pointLongitude = longitude ?: error("Longitud GPX inválida")
                    points += ImportedPoint(name, Coordinates(pointLatitude, pointLongitude), classify(name))
                    pointDepth = -1
                }
            }
        }
        return points
    }

    private fun classify(name: String?): RoutePointType {
        val normalized = name?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return when {
            normalized.startsWith("SALIDA") || normalized.startsWith("START") -> RoutePointType.START
            normalized.startsWith("META") || normalized.startsWith("FINISH") ||
                normalized.startsWith("LLEGADA") -> RoutePointType.FINISH
            normalized.startsWith("WPE") -> RoutePointType.ECLIPSE_WAYPOINT
            normalized.startsWith("CP") -> RoutePointType.CHECKPOINT
            normalized.startsWith("WP") -> RoutePointType.WAYPOINT
            else -> RoutePointType.OTHER
        }
    }

    private fun unzipKml(data: ByteArray): ByteArray {
        ZipInputStream(ByteArrayInputStream(data)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory || !entry.name.endsWith(".kml", ignoreCase = true)) {
                    zip.closeEntry()
                    continue
                }
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= MAX_FILE_BYTES) { "KML comprimido demasiado grande" }
                    output.write(buffer, 0, count)
                }
                return output.toByteArray()
            }
        }
        throw IllegalArgumentException("KMZ sin archivo KML")
    }

    private companion object { const val MAX_FILE_BYTES = 10 * 1024 * 1024 }
}
