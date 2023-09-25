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
import org.cosmicide.rewrite.common.Analytics
import java.io.File

class Download(val url: String, val callback: (percent: Int) -> Unit) {

    var totalBytes = 0.0
    var downloadedBytes = 0L

    fun start(file: File) {
        Analytics.logEvent("download", mapOf("url" to url, "file" to file.absolutePath))
        val request = Request.Builder()
            .url(url)
            .build()

        val response = OkHttpClient().newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Unexpected code $response")
        }

        totalBytes = response.body!!.contentLength().toDouble()
        if (totalBytes <= 0) {
            totalBytes = response.body!!.source().buffer.size.toDouble()
        }
        Log.d("Download", "Downloading $url to $file ($totalBytes B)")
        file.outputStream().use { out ->
            response.body!!.byteStream().buffered().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead = input.read(buffer)

                while (bytesRead != -1) {
                    out.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesRead = input.read(buffer)

                    val progress = ((downloadedBytes.toDouble() / totalBytes) * 100).toInt()
                    callback(progress)
                }
            }
        }
    }

    companion object {
        private const val BUFFER_SIZE = 512
    }
}