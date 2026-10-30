package org.solidcoder.trackchecker.tracks.application

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TrackImportBatchTest {
    @Test
    fun `processes all sources sequentially and isolates failures`() = runTest {
        val order = mutableListOf<Int>()
        val result = importSequentially(
            sources = listOf({ 1 }, { 2 }, { 3 }),
            onProgress = { completed, _ -> order += completed },
            process = { value -> if (value == 2) error("broken") }
        )

        assertEquals(TrackImportBatchResult(succeeded = 2, failed = 1), result)
        assertEquals(listOf(1, 2, 3), order)
    }

    @Test
    fun `does not swallow coroutine cancellation`() = runTest {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.test.runTest {
                importSequentially(
                    sources = listOf({ 1 }, { 2 }),
                    onProgress = { _, _ -> throw CancellationException("cancelled") },
                    process = {}
                )
            }
        }
    }
}
