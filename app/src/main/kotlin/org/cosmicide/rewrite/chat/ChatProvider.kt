/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.chat

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
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

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder().url(url).post(messagesJson.toRequestBody(mediaType)).build()

        val client = OkHttpClient().newBuilder().readTimeout(Duration.ofSeconds(120)).build()
        return try {
            val response = client.newCall(request).execute()
            val body = response.body.string()
            body
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}
