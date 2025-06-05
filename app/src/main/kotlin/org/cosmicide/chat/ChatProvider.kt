/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.chat

import android.R.attr.apiKey
import android.util.Log
import com.google.genai.Client
import com.google.genai.ResponseStream
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import org.cosmicide.BuildConfig
import org.cosmicide.common.Prefs
import java.util.concurrent.CompletableFuture

object ChatProvider {

    private val client = Client.builder()
        .apiKey(Prefs.geminiApiKey.ifEmpty { BuildConfig.GEMINI_API_KEY })
        .build()

    private var config = GenerateContentConfig.builder()
        .maxOutputTokens(Prefs.maxTokens)
        .temperature(Prefs.temperature)
        .topP(Prefs.topP)
        .topK(Prefs.topK)
        .build()

    private var chat =
        client.async.chats.create(Prefs.geminiModel.ifEmpty { "gemini-2.0-flash" }, config)

    fun regenerateModel(
        temp: Float = Prefs.temperature,
        top_p: Float = Prefs.topP,
        top_k: Float = Prefs.topK,
        maxTokens: Int = Prefs.maxTokens
    ) {
        Log.d(
            "ChatProvider",
            "regenerateModel: temperature ${Prefs.temperature}, topP ${Prefs.topP}, topK ${Prefs.topK}, maxTokens ${Prefs.maxTokens}"
        )

        config = GenerateContentConfig.builder()
            .maxOutputTokens(maxTokens)
            .temperature(temp)
            .topP(top_p)
            .topK(top_k)
            .build()

        chat = client.async.chats.create(Prefs.geminiModel.ifEmpty { "gemini-2.0-flash" }, config)
    }

    fun generate(conversation: List<Pair<String, String>>): CompletableFuture<ResponseStream<GenerateContentResponse>> {
        return chat.sendMessageStream(conversation.last().second)


        /*

        // json format
        // {
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

        val client = client.newBuilder().readTimeout(120, TimeUnit.SECONDS).build()
        return try {
            val response = client.newCall(request).execute()
            val body = response.body!!.string()
            response.close()
            body
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }

         */
    }
}
