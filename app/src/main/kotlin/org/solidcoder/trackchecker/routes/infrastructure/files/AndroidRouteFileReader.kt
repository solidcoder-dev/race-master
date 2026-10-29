package org.solidcoder.trackchecker.routes.infrastructure.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource
import org.solidcoder.trackchecker.tracks.application.ports.TrackImportSource

/** Reads a selected document once, without retaining an Android URI permission. */
class AndroidRouteFileReader(private val contentResolver: ContentResolver) {
    suspend fun read(uri: Uri): RouteImportSource = readFile(uri, 10 * 1024 * 1024).let { (name, bytes) ->
        RouteImportSource(name, bytes)
    }

    suspend fun readTrack(uri: Uri): TrackImportSource = readFile(uri, 20 * 1024 * 1024).let { (name, bytes) ->
        TrackImportSource(name, bytes)
    }

    private suspend fun readFile(uri: Uri, maximumBytes: Int): Pair<String, ByteArray> = withContext(Dispatchers.IO) {
        val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: throw IllegalArgumentException("No se pudo leer el nombre del archivo")
        val stream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("No se pudo abrir el archivo")
        val output = ByteArrayOutputStream()
        stream.use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead < 0) break
                require(output.size() + bytesRead <= maximumBytes) {
                    "El archivo supera el tamaño máximo permitido"
                }
                output.write(buffer, 0, bytesRead)
            }
        }
        name to output.toByteArray()
    }
}
