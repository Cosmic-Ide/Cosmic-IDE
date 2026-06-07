/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.util

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.cosmicide.common.Analytics
import java.io.File
import java.io.IOException

class Download(
    private val client: OkHttpClient,
    val url: String,
    private val callback: (percent: Int) -> Unit
) {

    private var totalBytes = -1L
    private var downloadedBytes = 0L

    /**
     * Downloads the file synchronously.
     * Note: Must be called from a background thread to prevent Android NetworkOnMainThreadException.
     */
    @Throws(IOException::class)
    fun start(file: File) {
        Analytics.logEvent("download", mapOf("url" to url, "file" to file.absolutePath))

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP code: ${response.code} for URL: $url")
            }

            val body = response.body ?: throw IOException("Response body is null for URL: $url")
            totalBytes = body.contentLength()

            Log.d(
                TAG,
                "Downloading $url to ${file.absolutePath} (Size: ${if (totalBytes > 0) "$totalBytes B" else "Unknown"})"
            )

            file.parentFile?.mkdirs()

            file.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var lastProgress = -1

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                callback(progress)
                            }
                        } else {
                            callback(-1)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "Download"
        private const val BUFFER_SIZE = 8192
    }
}