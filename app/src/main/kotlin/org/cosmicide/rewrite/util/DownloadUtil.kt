/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.util

import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class Download(val url: String, val callback: (percent: Int) -> Unit) {

    var totalBytes = 0L
    var downloadedBytes = 0L

    fun start(file: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept-Encoding", "gzip")
        connection.requestMethod = "GET"
        totalBytes = connection.contentLengthLong

        Log.d("Download", "Downloading $url to $file ($totalBytes B)")
        file.outputStream().use { out ->
            connection.inputStream.buffered().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead = input.read(buffer)

                while (bytesRead != -1) {
                    out.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesRead = input.read(buffer)

                    val progress = ((downloadedBytes.toDouble() / totalBytes) * 100).toInt()
                    val downloadedMB = downloadedBytes / (1024 * 1024)
                    val totalMB = totalBytes / (1024 * 1024)
                    Log.d(
                        "Download",
                        "downloadedMB: $downloadedMB MB, totalMB: $totalMB MB, progress: $progress%"
                    )
                    callback(progress)
                }
            }
        }
    }

    companion object {
        private const val BUFFER_SIZE = 4096
    }
}