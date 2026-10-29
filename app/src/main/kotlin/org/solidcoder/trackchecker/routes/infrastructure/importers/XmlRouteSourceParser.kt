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
import org.xmlpull.v1.XmlPullParser

/**
 * Imports route points only. Track geometries are intentionally ignored.
 * Parsing of source XML happens behind the application port, never in domain.
 */
class XmlRouteSourceParser(
    private val createParser: () -> XmlPullParser = { Xml.newPullParser() }
) : RouteSourceReader {
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
        val parser = createParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            // DTDs are rejected in the token stream; parsers vary in support for
            // explicitly setting the optional process-docdecl feature.
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
        var description: String? = null
        var explicitType: String? = null
        var typeDataDepth = -1

        while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.DOCDECL -> throw IllegalArgumentException("El archivo XML contiene una declaración no permitida")
                XmlPullParser.START_TAG -> when {
                    parser.name == "Placemark" -> {
                        placemarkDepth = parser.depth
                        pointDepth = -1
                        name = null
                        coordinates = null
                        description = null
                        explicitType = null
                        typeDataDepth = -1
                    }
                    placemarkDepth > 0 && parser.depth == placemarkDepth + 1 && parser.name == "name" -> {
                        name = parser.nextText().trim().takeIf { it.isNotEmpty() }
                    }
                    placemarkDepth > 0 && parser.depth == placemarkDepth + 1 && parser.name == "description" -> {
                        description = parser.nextText().trim()
                    }
                    placemarkDepth > 0 && parser.name == "Data" -> {
                        typeDataDepth = if (isTypeField(parser.getAttributeValue(null, "name"))) parser.depth else -1
                    }
                    placemarkDepth > 0 && parser.name == "SimpleData" &&
                        isTypeField(parser.getAttributeValue(null, "name")) -> {
                        explicitType = parser.nextText().trim()
                    }
                    typeDataDepth > 0 && parser.depth == typeDataDepth + 1 && parser.name == "value" -> {
                        explicitType = parser.nextText().trim()
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
                    coordinates?.let { points += parseKmlPoint(name, it, description, explicitType) }
                    placemarkDepth = -1
                    pointDepth = -1
                    typeDataDepth = -1
                } else if (parser.name == "Data" && parser.depth == typeDataDepth) {
                    typeDataDepth = -1
                }
            }
        }
        return points
    }

    private fun parseKmlPoint(
        name: String?, rawCoordinates: String, description: String?, explicitType: String?
    ): ImportedPoint {
        val components = rawCoordinates.split(Regex("[,\\s]+"))
        require(components.size >= 2) { "Coordenadas KML incompletas" }
        val longitude = components[0].toDoubleOrNull() ?: error("Longitud KML inválida")
        val latitude = components[1].toDoubleOrNull() ?: error("Latitud KML inválida")
        return ImportedPoint(name, Coordinates(latitude, longitude), ImportedPointTypeClassifier.classify(name, description, explicitType))
    }

    private fun parseGpx(parser: XmlPullParser): List<ImportedPoint> {
        val points = mutableListOf<ImportedPoint>()
        var pointDepth = -1
        var latitude: Double? = null
        var longitude: Double? = null
        var name: String? = null
        var description: String? = null
        var explicitType: String? = null

        while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.DOCDECL -> throw IllegalArgumentException("El archivo XML contiene una declaración no permitida")
                XmlPullParser.START_TAG -> when {
                    parser.name == "wpt" || parser.name == "rtept" -> {
                        pointDepth = parser.depth
                        latitude = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        longitude = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        name = null
                        description = null
                        explicitType = null
                    }
                    pointDepth > 0 && parser.depth == pointDepth + 1 && parser.name == "name" -> {
                        name = parser.nextText().trim().takeIf { it.isNotEmpty() }
                    }
                    pointDepth > 0 && parser.depth == pointDepth + 1 && parser.name == "desc" -> {
                        description = parser.nextText().trim()
                    }
                    pointDepth > 0 && parser.depth == pointDepth + 1 && parser.name == "type" -> {
                        explicitType = parser.nextText().trim()
                    }
                }
                XmlPullParser.END_TAG -> if (
                    (parser.name == "wpt" || parser.name == "rtept") && parser.depth == pointDepth
                ) {
                    val pointLatitude = latitude ?: error("Latitud GPX inválida")
                    val pointLongitude = longitude ?: error("Longitud GPX inválida")
                    points += ImportedPoint(name, Coordinates(pointLatitude, pointLongitude), ImportedPointTypeClassifier.classify(name, description, explicitType))
                    pointDepth = -1
                }
            }
        }
        return points
    }

    private fun isTypeField(name: String?): Boolean =
        name?.lowercase(Locale.ROOT) in setOf("type", "pointtype", "point_type", "category")

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
