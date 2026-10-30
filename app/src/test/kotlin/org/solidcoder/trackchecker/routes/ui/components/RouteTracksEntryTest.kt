package org.solidcoder.trackchecker.routes.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTracksEntryTest {
    @Test
    fun `empty entry explains that tracks can be imported`() {
        assertEquals(TracksEntryCopy("Tracks", "Todavía no hay recorridos", "Importar tracks"), tracksEntryCopy(0))
    }

    @Test
    fun `populated entry shows evaluated count`() {
        assertEquals(TracksEntryCopy("Tracks", "12 recorridos evaluados", "Abrir"), tracksEntryCopy(12))
    }

    @Test
    fun `tracks entry is visible only after route validation`() {
        assertEquals(true, shouldShowTracksEntry(routeReady = true))
        assertEquals(false, shouldShowTracksEntry(routeReady = false))
    }

    @Test
    fun `track count does not make an unvalidated route visible`() {
        assertEquals(false, shouldShowTracksEntry(routeReady = false))
    }
}
