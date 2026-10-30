package org.solidcoder.trackchecker.tracks.infrastructure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.kxml2.io.KXmlParser
import org.solidcoder.trackchecker.tracks.application.ports.TrackImportSource
import org.solidcoder.trackchecker.tracks.infrastructure.importers.XmlTrackSourceParser

class XmlTrackSourceParserTest {
    private val parser = XmlTrackSourceParser { KXmlParser() }

    @Test fun `GPX preserves track segment boundaries and sample order`() {
        val xml = """<gpx><trk><trkseg>
          <trkpt lat="28.0" lon="-15.0"><time>2024-01-01T10:00:00Z</time></trkpt>
          <trkpt lat="28.1" lon="-15.1"/>
          </trkseg><trkseg><trkpt lat="28.2" lon="-15.2"/></trkseg></trk></gpx>"""
        val segments = parser.read(TrackImportSource("participant.gpx", xml.toByteArray()))
        assertEquals(listOf(2, 1), segments.map { it.samples.size })
        assertEquals(28.0, segments[0].samples.first().latitude, 0.0)
        assertEquals(28.2, segments[1].samples.first().latitude, 0.0)
        assertEquals(1704103200000L, segments[0].samples.first().timestampEpochMs)
    }

    @Test fun `GPX route geometry is a fallback when track geometry is absent`() {
        val xml = """<gpx><wpt lat="28" lon="-15"/><rte>
          <rtept lat="28.1" lon="-15.1"/><rtept lat="28.2" lon="-15.2"/>
          </rte></gpx>"""
        val segments = parser.read(TrackImportSource("route.gpx", xml.toByteArray()))
        assertEquals(2, segments.single().samples.size)
    }

    @Test fun `isolated GPX waypoints are not treated as a recorded track`() {
        assertThrows(IllegalArgumentException::class.java) {
            parser.read(TrackImportSource("points.gpx", "<gpx><wpt lat=\"28\" lon=\"-15\"/></gpx>".toByteArray()))
        }
    }

    @Test fun `KML reads LineString but not isolated placemark points`() {
        val xml = """<kml><Document><Placemark><Point><coordinates>-15,28</coordinates></Point></Placemark>
          <Placemark><LineString><coordinates>-15,28,0 -15.1,28.1,0 -15.2,28.2</coordinates></LineString></Placemark>
          </Document></kml>"""
        val segments = parser.read(TrackImportSource("track.kml", xml.toByteArray()))
        assertEquals(3, segments.single().samples.size)
        assertEquals(-15.2, segments.single().samples.last().longitude, 0.0)
    }

    @Test fun `KML ignores undeclared vendor extensions around track geometry`() {
        val xml = """<kml><Document><Placemark>
          <vendor:metadata><vendor:name>recorded</vendor:name></vendor:metadata>
          <LineString><coordinates>-15,28 -15.1,28.1</coordinates></LineString>
          </Placemark></Document></kml>"""
        val segments = parser.read(TrackImportSource("track.kml", xml.toByteArray()))
        assertEquals(2, segments.single().samples.size)
    }

    @Test fun `KML gx Track preserves ordered coordinates and timestamps`() {
        val xml = """<kml xmlns:gx="http://www.google.com/kml/ext/2.2"><Placemark><gx:Track>
          <when>2024-01-01T10:00:00Z</when><when>2024-01-01T10:01:00Z</when>
          <gx:coord>-15 28 0</gx:coord><gx:coord>-15.1 28.1 0</gx:coord>
          </gx:Track></Placemark></kml>"""
        val samples = parser.read(TrackImportSource("track.kml", xml.toByteArray())).single().samples
        assertEquals(2, samples.size)
        assertEquals(1704103260000L, samples.last().timestampEpochMs)
    }

    @Test fun `KML vendor geometry names are not treated as supported track geometry`() {
        val xml = """<kml><vendor:Track>
          <vendor:coord>-15 28 0</vendor:coord><vendor:coord>-15.1 28.1 0</vendor:coord>
          </vendor:Track></kml>"""
        assertThrows(IllegalArgumentException::class.java) {
            parser.read(TrackImportSource("track.kml", xml.toByteArray()))
        }
    }

    @Test fun `KMZ imports the contained track KML`() {
        val zipped = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("doc.kml"))
                zip.write("<kml><LineString><coordinates>-15,28 -15.1,28.1</coordinates></LineString></kml>".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        assertEquals(2, parser.read(TrackImportSource("track.kmz", zipped)).single().samples.size)
    }
}
