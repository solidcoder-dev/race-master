package org.picolobruno.racing.infrastructure

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.picolobruno.racing.domain.Checkpoint
import org.picolobruno.racing.domain.Coordinate
import org.picolobruno.racing.domain.RaceRoute
import org.picolobruno.racing.domain.Score
import org.picolobruno.racing.domain.Track
import org.picolobruno.racing.domain.Waypoint
import org.w3c.dom.Document
import org.w3c.dom.Node

/**
 * Infrastructure service responsible for parsing KML files.
 * It provides:
 *   - parseTrack(...)     -> to build a [Track]
 *   - parseRaceRoute(...) -> to build a [RaceRoute]
 */
object KmlParser {

    /**
     * Parses the first <Placemark> with a <LineString> from [filePath]
     * and returns a [Track]. If not found, returns an empty Track or
     * throws an exception (your call).
     */
    fun parseTrack(filePath: String): Track {
        val document = parseXmlDocument(filePath)
        val placemarks = document.getElementsByTagName("Placemark")

        for (i in 0 until placemarks.length) {
            val placemarkNode = placemarks.item(i)

            // Look for <LineString> child
            val lineStringNodes = placemarkNode.findChildNodesByName("LineString")
            if (lineStringNodes.isNotEmpty()) {
                // We'll assume there's only one <LineString> in this placemark
                val lineStringNode = lineStringNodes.first()
                val coordsNode = lineStringNode.findChildNodesByName("coordinates").firstOrNull()
                    ?: continue

                val coordsText = coordsNode.textContent?.trim() ?: continue

                // Each coordinate is in "lon,lat[,alt]" format
                val coordinateList = coordsText
                    .split("\\s+".toRegex())
                    .filter { it.isNotEmpty() }
                    .mapNotNull { parseCoordinate(it) }

                return Track(coordinateList)
            }
        }

        // If no <LineString> found
        return Track(emptyList())
        // or throw IllegalStateException("No <LineString> found in $filePath")
    }

    /**
     * Parses a RaceRoute from [filePath]:
     *   - "SALIDA ET1" for the start coordinate
     *   - "META ET1"   for the end coordinate
     *   - "WPxxx"      as waypoints (scores from [waypointScores])
     *   - "CPxxx"      as checkpoints
     *
     * @param waypointScores -> Map of "WP1" -> Score(10), ...
     */
    fun parseRaceRoute(
        filePath: String,
        waypointScores: Map<String, Score>
    ): RaceRoute {
        val document = parseXmlDocument(filePath)
        val placemarks = document.getElementsByTagName("Placemark")

        var startCoordinate: Coordinate? = null
        var endCoordinate: Coordinate? = null
        val waypoints = mutableListOf<Waypoint>()
        val checkpoints = mutableListOf<Checkpoint>()

        // For each placemark, read the <name> and the <Point>'s <coordinates>
        for (i in 0 until placemarks.length) {
            val placemarkNode = placemarks.item(i)

            val name = readName(placemarkNode) ?: continue
            val coordinate = readPointCoordinate(placemarkNode) ?: continue

            when {
                // Start
                name == "SALIDA ET1" -> startCoordinate = coordinate

                // End
                name == "META ET1" -> endCoordinate = coordinate

                // Waypoint
                name.startsWith("WP") -> {
                    val score = waypointScores[name] ?: Score(0)
                    waypoints.add(Waypoint(name, coordinate, score))
                }

                // Checkpoint
                name.startsWith("CP") -> {
                    checkpoints.add(Checkpoint(name, coordinate))
                }

                // Else ignore or handle differently
            }
        }

        val finalStart = startCoordinate
            ?: throw IllegalStateException("No placemark named 'SALIDA ET1' found.")
        val finalEnd = endCoordinate
            ?: throw IllegalStateException("No placemark named 'META ET1' found.")

        return RaceRoute(
            start = finalStart,
            end = finalEnd,
            waypoints = waypoints,
            checkpoints = checkpoints
        )
    }

    // ---------------------------------------
    //               Helpers
    // ---------------------------------------

    /**
     * Build a W3C Document from the KML file.
     */
    private fun parseXmlDocument(filePath: String): Document {
        val file = File(filePath)
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(file)
    }

    /**
     * Reads the <name> text from a <Placemark> node. Returns null if missing.
     */
    private fun readName(placemarkNode: Node): String? {
        val nameNodes = placemarkNode.findChildNodesByName("name")
        return nameNodes.firstOrNull()?.textContent?.trim()
    }

    /**
     * Reads the first <Point> -> <coordinates> from [placemarkNode].
     * Returns a [Coordinate] or null if absent.
     */
    private fun readPointCoordinate(placemarkNode: Node): Coordinate? {
        val pointNodes = placemarkNode.findChildNodesByName("Point")
        if (pointNodes.isEmpty()) return null

        // We'll assume there's only one <Point> in this placemark
        val pointNode = pointNodes.first()
        val coordsNode = pointNode.findChildNodesByName("coordinates").firstOrNull() ?: return null

        val coordsString = coordsNode.textContent?.trim() ?: return null
        return parseCoordinate(coordsString)
    }

    /**
     * Parse "lon,lat[,alt]" into a [Coordinate], ignoring altitude.
     * Returns null if malformed.
     */
    private fun parseCoordinate(coordString: String): Coordinate? {
        val parts = coordString.split(",")
        val lon = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
        val lat = parts.getOrNull(1)?.toDoubleOrNull() ?: return null

        return Coordinate(latitude = lat, longitude = lon)
    }

    /**
     * Utility: find child nodes by tag name (case-insensitive).
     */
    private fun Node.findChildNodesByName(name: String): List<Node> {
        val nodeList = this.childNodes
        val result = mutableListOf<Node>()
        for (i in 0 until nodeList.length) {
            val child = nodeList.item(i)
            if (child.nodeName.equals(name, ignoreCase = true)) {
                result.add(child)
            }
        }
        return result
    }
}
