/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.chat

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration

object ChatProvider {

    private val gson = Gson()

    data class Message(val role: String, val content: String)

    @JvmStatic
    fun generate(model: String, conversation: List<Map<String, String>>): String {
        // json format: {
        //  "messages": [
        //    {"role": "user", "content": "hi"},
        //    {"role": "bot", "content": "hello"}
        //  ]
        //}
        val messages = conversation.map { Message(it["author"]!!, it["text"]!!) }
        val jsonObject = mapOf("messages" to messages)
        val messagesJson = gson.toJson(jsonObject)
        println(messagesJson)
        val url = "https://gpt4free-experimental.pranavpurwar.repl.co/chat/$model"
        Log.d("ChatProvider", "generate: $url")

        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = messagesJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient().newBuilder().readTimeout(Duration.ofSeconds(120)).build()
        return try {
            val response = client.newCall(request).execute()
            val body = response.body.string()
            response.close()
            body
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        } finally {
            client.connectionPool.evictAll()
        }
    }
}
