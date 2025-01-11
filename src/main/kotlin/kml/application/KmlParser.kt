package org.picolobruno.racing.kml.application

import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.picolobruno.racing.kml.domain.KmlDocument
import org.picolobruno.racing.kml.domain.Placemark
import org.w3c.dom.Document
import org.w3c.dom.Element

// TODO: verify if the kml package is adapted to the DDD architecture (location, interaction, ...)

class KmlParser {

    fun parse(input: InputStream): KmlDocument {
        val doc = parseXmlDocument(input)
        val documentName = extractDocumentName(doc)
        val placemarks = extractPlacemarks(doc)
        return KmlDocument(
            name = documentName,
            placemarks = placemarks
        )
    }

    private fun parseXmlDocument(input: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false // ignoring namespaces for simplicity

        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(input)
        doc.documentElement.normalize()

        return doc
    }

    private fun extractDocumentName(doc: Document): String {
        val documentNodes = doc.getElementsByTagName("Document")

        if (documentNodes.length == 0) {
            return "NO_DOCUMENT_FOUND" // TODO: throw an exception?
        }

        val documentElement = documentNodes.item(0) as Element
        val nameNodes = documentElement.getElementsByTagName("name")

        return if (nameNodes.length > 0) {
            nameNodes.item(0).textContent
        } else {
            "NO_NAME_FOUND" // TODO: throw an exception??
        }
    }


    private fun extractPlacemarks(doc: Document): List<Placemark> {
        val placemarkList = mutableListOf<Placemark>()
        val placemarkNodes = doc.getElementsByTagName("Placemark")

        for (i in 0 until placemarkNodes.length) {
            val placemarkElement = placemarkNodes.item(i) as? Element ?: continue

            val name = extractPlacemarkName(placemarkElement)
            val (longitude, latitude) = extractPlacemarkCoordinates(placemarkElement)

            placemarkList.add(Placemark(name, longitude, latitude))
        }

        return placemarkList
    }

    private fun extractPlacemarkName(placemarkElement: Element): String {
        val nameNodes = placemarkElement.getElementsByTagName("name")
        if (nameNodes.length == 0) {
            return "NO_NAME_FOUND"
        }
        return nameNodes.item(0).textContent.trim()
    }

    private fun extractPlacemarkCoordinates(placemarkElement: Element): Pair<Double, Double> {
        val pointNodes = placemarkElement.getElementsByTagName("Point")
        if (pointNodes.length == 0) return 0.0 to 0.0

        val pointElement = pointNodes.item(0) as Element
        val coordsNodes = pointElement.getElementsByTagName("coordinates")
        if (coordsNodes.length == 0) return 0.0 to 0.0

        val coordsText = coordsNodes.item(0).textContent.trim()
        val parts = coordsText.split(",")
        val longitude = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        val latitude = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0

        return longitude to latitude
    }

    companion object {
        fun parse(kml: String): KmlDocument {
            val parser = KmlParser()
            return ByteArrayInputStream(kml.toByteArray()).use {
                parser.parse(it)
            }
        }
    }
}
