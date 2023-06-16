/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.chat

import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.random.Random

object ChatCompletion {
    @JvmStatic
    val client = OkHttpClient()

    fun md5(text: String): String {
        return text.encodeToByteArray().let {
            val digest = java.security.MessageDigest.getInstance("MD5")
            digest.update(it)
            val bytes = digest.digest()
            val sb = StringBuilder()
            for (byte in bytes) {
                sb.append(String.format("%02X", byte))
            }
            sb.toString().reversed()
        }
    }

    @JvmStatic
    fun getApiKey(userAgent: String): String {
        val part1 = Random.nextLong(0, 10_000_000_000_000)
        val part2 = md5(userAgent + md5(userAgent + md5(userAgent + part1.toString() + "x")))
        return "tryit-$part1-$part2"
    }

    @JvmStatic
    fun create(messages: List<Map<String, String>>, callback: (String) -> Unit) {
        val userAgent = UserAgent.random()
        val apiKey = getApiKey(userAgent)
        val headers = hashMapOf(
            "api-key" to apiKey,
            "user-agent" to userAgent
        )
        val files = hashMapOf(
            "chat_style" to "chat",
            "chatHistory" to JSONObject().apply { put("messages", messages) }.toString()
        )

        val requestBody = HashMap<String, Any>()
        requestBody.putAll(files)

        val multipartBuilder = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)

        for ((key, value) in requestBody) {
            multipartBuilder.addFormDataPart(
                key,
                null,
                value.toString().toRequestBody("application/json".toMediaType())
            )
        }

        val request = Request.Builder()
            .url("https://api.deepai.org/chat_response")
            .headers(headers.toHeaders())
            .post(multipartBuilder.build())
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val content = response.body.string()
                callback(content)
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback("")
            }
        })
    }
}

