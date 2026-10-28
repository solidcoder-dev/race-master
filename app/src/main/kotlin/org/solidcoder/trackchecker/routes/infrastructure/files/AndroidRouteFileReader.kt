package org.solidcoder.trackchecker.routes.infrastructure.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.solidcoder.trackchecker.routes.application.ports.RouteImportSource

/** Reads a selected document once, without retaining an Android URI permission. */
class AndroidRouteFileReader(private val contentResolver: ContentResolver) {
    suspend fun read(uri: Uri): RouteImportSource = withContext(Dispatchers.IO) {
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
                require(output.size() + bytesRead <= 10 * 1024 * 1024) {
                    "El archivo supera 10 MB"
                }
                output.write(buffer, 0, bytesRead)
            }
        }
        RouteImportSource(name, output.toByteArray())
    }
}
