package org.solidcoder.trackchecker.tracks.application

import kotlinx.coroutines.CancellationException

data class TrackImportBatchResult(val succeeded: Int, val failed: Int)

suspend fun <T> importSequentially(
    sources: List<suspend () -> T>,
    onProgress: suspend (completed: Int, total: Int) -> Unit,
    process: suspend (T) -> Unit
): TrackImportBatchResult {
    var succeeded = 0
    var failed = 0
    sources.forEachIndexed { index, sourceFactory ->
        onProgress(index + 1, sources.size)
        try {
            process(sourceFactory())
            succeeded++
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            failed++
        }
    }
    return TrackImportBatchResult(succeeded, failed)
}
