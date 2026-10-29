package org.solidcoder.trackchecker.routes.infrastructure

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.solidcoder.trackchecker.routes.domain.model.RoutePointType
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource
import org.solidcoder.trackchecker.routes.application.ports.RouteSourceReader
import org.solidcoder.trackchecker.routes.infrastructure.importers.XmlRouteSourceParser
import org.kxml2.io.KXmlParser

class XmlRouteSourceParserTest {
    private val parser: RouteSourceReader = XmlRouteSourceParser { KXmlParser() }

    @Test fun `real Nomad KML contains 63 points including distinct duplicate WP04 labels`() {
        val source = checkNotNull(javaClass.classLoader?.getResourceAsStream("ET 1 WP NOMAD 2024.kml"))
        val points = source.use { parser.read(RouteImportSource("etapa.kml", it.readBytes())) }
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
        val parsed = parser.read(RouteImportSource("route.kml", kml.toByteArray()))
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
        val points = parser.read(RouteImportSource("stage.gpx", gpx.toByteArray()))
        assertEquals(2, points.size)
        assertEquals(RoutePointType.ECLIPSE_WAYPOINT, points[1].type)
    }

    @Test fun `KML structured type takes priority over a misleading name`() {
        val kml = """<kml><Placemark><name>WP01</name><ExtendedData>
            <Data name="pointType"><value>checkpoint</value></Data>
            </ExtendedData><Point><coordinates>-15,28</coordinates></Point></Placemark></kml>"""
        val point = parser.read(RouteImportSource("route.kml", kml.toByteArray())).single()
        assertEquals(RoutePointType.CHECKPOINT, point.type)
        assertEquals("WP01", point.name)
    }

    @Test fun `KML schema data and description classify unnamed-type points`() {
        val kml = """<kml><Document>
            <Placemark><name>Point A</name><ExtendedData><SchemaData>
              <SimpleData name="type">start</SimpleData>
            </SchemaData></ExtendedData><Point><coordinates>-15,28</coordinates></Point></Placemark>
            <Placemark><name>Point B</name><description>Checkpoint</description>
              <Point><coordinates>-15.1,28.1</coordinates></Point></Placemark>
            </Document></kml>"""
        val points = parser.read(RouteImportSource("route.kml", kml.toByteArray()))
        assertEquals(listOf(RoutePointType.START, RoutePointType.CHECKPOINT), points.map { it.type })
    }

    @Test fun `GPX type takes priority over waypoint name`() {
        val gpx = """<gpx><wpt lat="28" lon="-15"><name>WP01</name><type>checkpoint</type></wpt></gpx>"""
        assertEquals(RoutePointType.CHECKPOINT,
            parser.read(RouteImportSource("route.gpx", gpx.toByteArray())).single().type)
    }

    @Test fun `KMZ extracts its embedded KML`() {
        val zipped = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("doc.kml"))
                zip.write("<kml><Placemark><name>CP1</name><Point><coordinates>-15,28</coordinates></Point></Placemark></kml>".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        val points = parser.read(RouteImportSource("stage.kmz", zipped))
        assertEquals(1, points.size)
        assertEquals(RoutePointType.CHECKPOINT, points.single().type)
    }

    @Test fun `external entities are rejected`() {
        val xml = """<!DOCTYPE kml [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><kml><Placemark><name>&xxe;</name><Point><coordinates>-15,28</coordinates></Point></Placemark></kml>"""
        assertThrows(Exception::class.java) { parser.read(RouteImportSource("unsafe.kml", xml.toByteArray())) }
    }

    @Test fun `malformed coordinates do not produce silently corrupted points`() {
        val xml = "<kml><Placemark><name>WP1</name><Point><coordinates>abc,28</coordinates></Point></Placemark></kml>"
        assertThrows(Exception::class.java) { parser.read(RouteImportSource("bad.kml", xml.toByteArray())) }
    }
}
