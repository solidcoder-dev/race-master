package org.solidcoder.trackchecker.routes.infrastructure.importers

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.solidcoder.trackchecker.routes.application.ports.ImportedPoint
import org.solidcoder.trackchecker.routes.application.ports.RouteSourceReader
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource
import org.solidcoder.trackchecker.routes.domain.model.Coordinates
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.w3c.dom.Element
import org.w3c.dom.Node

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
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
            // Android's XMLConstants API omits these desktop Java fields.
            // Keep DTD rejection mandatory; extra JAXP attributes are best effort.
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
        }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        val root = document.documentElement?.localName?.lowercase(Locale.ROOT)
        return when (root) {
            "kml" -> parseKml(document.documentElement)
            "gpx" -> parseGpx(document.documentElement)
            else -> throw IllegalArgumentException("El archivo no contiene un documento KML o GPX")
        }
    }

    private fun parseKml(root: Element): List<ImportedPoint> =
        root.getElementsByTagNameNS("*", "Placemark").asElements().mapNotNull { placemark ->
            // Only a direct Point geometry belongs to an official route waypoint.
            val point = placemark.directChild("Point") ?: return@mapNotNull null
            val rawCoordinates = point.directChild("coordinates")?.textContent?.trim()
                ?: throw IllegalArgumentException("Un punto KML no tiene coordenadas")
            val components = rawCoordinates.split(Regex("[,\\s]+"))
            require(components.size >= 2) { "Coordenadas KML incompletas" }
            val longitude = components[0].toDoubleOrNull() ?: error("Longitud KML inválida")
            val latitude = components[1].toDoubleOrNull() ?: error("Latitud KML inválida")
            val name = placemark.directChild("name")?.textContent?.trim()
            ImportedPoint(name, Coordinates(latitude, longitude), classify(name))
        }

    private fun parseGpx(root: Element): List<ImportedPoint> {
        val candidates = root.getElementsByTagNameNS("*", "wpt").asElements() +
            root.getElementsByTagNameNS("*", "rtept").asElements()
        return candidates.map { element ->
            val latitude = element.getAttribute("lat").toDoubleOrNull()
                ?: error("Latitud GPX inválida")
            val longitude = element.getAttribute("lon").toDoubleOrNull()
                ?: error("Longitud GPX inválida")
            val name = element.directChild("name")?.textContent?.trim()
            ImportedPoint(name, Coordinates(latitude, longitude), classify(name))
        }
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

    private fun Element.directChild(local: String): Element? =
        (0 until childNodes.length).map { childNodes.item(it) }
            .filterIsInstance<Element>()
            .firstOrNull { it.localName == local || it.nodeName == local }

    private fun org.w3c.dom.NodeList.asElements(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

    private companion object { const val MAX_FILE_BYTES = 10 * 1024 * 1024 }
}
