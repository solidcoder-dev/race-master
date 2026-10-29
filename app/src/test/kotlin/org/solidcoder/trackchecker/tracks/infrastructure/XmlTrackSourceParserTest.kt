package org.solidcoder.trackchecker.tracks.infrastructure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
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
}
