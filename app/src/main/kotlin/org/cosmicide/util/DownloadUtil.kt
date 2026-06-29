/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.util

import android.util.Log
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cosmicide.common.Analytics
import java.io.File
import java.io.IOException

class Download(
    private val client: HttpClient,
    val url: String,
    private val callback: (downloadedBytes: Long, totalBytes: Long) -> Unit
) {

    /**
     * Downloads the file asynchronously utilizing Ktor and Kotlin Coroutines.
     * Safely dispatches I/O context tasks off the main runtime execution loop.
     */
    @Throws(IOException::class)
    suspend fun start(file: File) = withContext(Dispatchers.IO) {
        Analytics.logEvent("download", mapOf("url" to url, "file" to file.absolutePath))

        try {
            file.parentFile?.mkdirs()

            client.prepareGet(url) {
                timeout {
                    requestTimeoutMillis = 300_000
                    connectTimeoutMillis = 5_000
                }

                onDownload { bytesSentTotal, contentLength ->
                    callback(bytesSentTotal, contentLength ?: -1L)
                }
            }.execute { response ->
                if (response.status.value !in 200..299) {
                    throw IOException("Unexpected HTTP code: ${response.status} for URL: $url")
                }

                val channel = response.bodyAsChannel()

                file.outputStream().use { outputStream ->
                    val buffer = ByteArray(131072) // 128 KB chunk windows
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read == -1) break
                        if (read > 0) {
                            outputStream.write(buffer, 0, read)
                        }
                    }
                }
            }

            Log.d(TAG, "Successfully downloaded $url to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling network stream transfer for $url", e)
            throw IOException("Download failed: ${e.localizedMessage}", e)
        }
    }

    companion object {
        private const val TAG = "Download"
    }
}