package org.picolobruno.trackchecker.routes.infrastructure

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.picolobruno.trackchecker.routes.domain.model.RoutePointType
import org.picolobruno.trackchecker.routes.infrastructure.importers.XmlRouteSourceParser

class XmlRouteSourceParserTest {
    private val parser = XmlRouteSourceParser()

    @Test fun `real Nomad KML contains 63 points including distinct duplicate WP04 labels`() {
        val source = checkNotNull(javaClass.classLoader?.getResourceAsStream("ET 1 WP NOMAD 2024.kml"))
        val points = source.use { parser.parse("etapa.kml", it.readBytes()) }
        assertEquals(63, points.size)
        assertEquals(2, points.count { it.name == "WP04" })
        assertEquals(1, points.count { it.type == RoutePointType.START })
        assertEquals(1, points.count { it.type == RoutePointType.FINISH })
        assertTrue(points.first().type != RoutePointType.START)
        assertTrue(points.last().type != RoutePointType.FINISH)
    }

    @Test fun `KML routes parse Point placemarks and ignore LineStrings`() {
        val kml = """
            <kml xmlns="http://www.opengis.net/kml/2.2"><Document>
              <Placemark><name>SALIDA ET1</name><Point><coordinates>-15.0,28.0,0</coordinates></Point></Placemark>
              <Placemark><name>ignored track</name><LineString><coordinates>-15,28 -16,29</coordinates></LineString></Placemark>
              <Placemark><name>WP01</name><Point><coordinates>-15.1,28.1</coordinates></Point></Placemark>
            </Document></kml>
        """.trimIndent()
        val parsed = parser.parse("route.kml", kml.toByteArray())
        assertEquals(2, parsed.size)
        assertEquals(RoutePointType.START, parsed[0].type)
        assertEquals(RoutePointType.WAYPOINT, parsed[1].type)
        assertEquals(28.1, parsed[1].coordinates.latitude, 0.000001)
    }

    @Test fun `GPX route imports both waypoint and route point definitions`() {
        val gpx = """
            <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
              <wpt lat="28.0" lon="-15.0"><name>SALIDA</name></wpt>
              <rte><rtept lat="28.1" lon="-15.1"><name>WPE1</name></rtept></rte>
              <trk><trkseg><trkpt lat="28.2" lon="-15.2"/></trkseg></trk>
            </gpx>
        """.trimIndent()
        val points = parser.parse("stage.gpx", gpx.toByteArray())
        assertEquals(2, points.size)
        assertEquals(RoutePointType.EXIT_WAYPOINT, points[1].type)
    }

    @Test fun `KMZ extracts its embedded KML`() {
        val zipped = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("doc.kml"))
                zip.write("<kml><Placemark><name>CP1</name><Point><coordinates>-15,28</coordinates></Point></Placemark></kml>".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        val points = parser.parse("stage.kmz", zipped)
        assertEquals(1, points.size)
        assertEquals(RoutePointType.CHECKPOINT, points.single().type)
    }

    @Test fun `external entities are rejected`() {
        val xml = """<!DOCTYPE kml [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><kml><Placemark><name>&xxe;</name><Point><coordinates>-15,28</coordinates></Point></Placemark></kml>"""
        assertThrows(Exception::class.java) { parser.parse("unsafe.kml", xml.toByteArray()) }
    }

    @Test fun `malformed coordinates do not produce silently corrupted points`() {
        val xml = "<kml><Placemark><name>WP1</name><Point><coordinates>abc,28</coordinates></Point></Placemark></kml>"
        assertThrows(Exception::class.java) { parser.parse("bad.kml", xml.toByteArray()) }
    }
}
