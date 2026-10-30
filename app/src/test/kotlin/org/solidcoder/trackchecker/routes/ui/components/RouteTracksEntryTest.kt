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
    fun `validated routes never show a validation or import button in the bottom sheet`() {
        assertEquals(false, shouldShowValidationButton(trackCount = 0, routeReady = true, canValidate = true))
        assertEquals(false, shouldShowValidationButton(trackCount = 12, routeReady = true, canValidate = true))
    }

    @Test
    fun `draft routes keep validation available even when tracks already exist`() {
        assertEquals(true, shouldShowValidationButton(trackCount = 12, routeReady = false, canValidate = true))
        assertEquals(false, shouldShowValidationButton(trackCount = 12, routeReady = false, canValidate = false))
    }
}
