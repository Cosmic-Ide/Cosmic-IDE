/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.chat

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import org.cosmicide.BuildConfig
import org.cosmicide.common.Prefs

object ChatProvider {

    private val safetySettings = listOf(
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE), // should we block this?
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
    )

    private var generativeModel = GenerativeModel(
        apiKey = BuildConfig.GEMINI_API_KEY,
        modelName = "gemini-1.0-pro-latest",
        safetySettings = safetySettings,
        generationConfig = generationConfig {
            temperature = Prefs.temperature
            topP = Prefs.topP
            topK = Prefs.topK
            maxOutputTokens = Prefs.maxTokens
        }
    )

    private var chat = generativeModel.startChat()

    fun regenerateModel(
        temp: Float = Prefs.temperature,
        top_p: Float = Prefs.topP,
        top_k: Int = Prefs.topK,
        maxTokens: Int = Prefs.maxTokens
    ) {
        Log.d(
            "ChatProvider",
            "regenerateModel: temperature ${Prefs.temperature}, topP ${Prefs.topP}, topK ${Prefs.topK}, maxTokens ${Prefs.maxTokens}"
        )

        GenerativeModel(
            apiKey = Prefs.geminiApiKey.ifEmpty { BuildConfig.GEMINI_API_KEY },
            modelName = "gemini-pro",
            safetySettings = safetySettings,
            generationConfig = generationConfig {
                temperature = temp
                topP = top_p
                topK = top_k
                maxOutputTokens = maxTokens
            }
        ).let {
            generativeModel = it
            chat = it.startChat()
        }
    }

    fun generate(conversation: List<Map<String, String>>): Flow<GenerateContentResponse> {
        return chat.sendMessageStream(conversation.last()["text"]!!)


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
