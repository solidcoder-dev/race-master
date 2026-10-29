package org.solidcoder.trackchecker.tracks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.solidcoder.trackchecker.tracks.domain.model.GpsSample
import org.solidcoder.trackchecker.tracks.domain.model.Track
import org.solidcoder.trackchecker.tracks.domain.model.TrackId
import org.solidcoder.trackchecker.tracks.domain.model.TrackSegment

class TrackTest {
    @Test fun `samples retain their recorded order and duplicate timestamps`() {
        val first = GpsSample(28.0, -15.0, 100L)
        val second = GpsSample(28.1, -15.1, 100L)
        val track = Track(TrackId("track-1"), "Participant", listOf(TrackSegment(listOf(first, second))))

        assertEquals(listOf(first, second), track.segments.single().samples)
    }

    @Test fun `a track cannot consist of empty segments`() {
        assertThrows(IllegalArgumentException::class.java) {
            Track(TrackId("track-1"), "Participant", listOf(TrackSegment(emptyList())))
        }
    }

    @Test fun `coordinates must be within geographical bounds`() {
        assertThrows(IllegalArgumentException::class.java) { GpsSample(100.0, 0.0) }
    }
}
