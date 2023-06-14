/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package com.pkslow.ai.util

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.pkslow.ai.domain.Answer
import com.pkslow.ai.domain.AnswerStatus
import com.pkslow.ai.domain.BardRequest
import com.pkslow.ai.domain.BardResponse
import com.pkslow.ai.util.Constants.ASK_QUESTION_PATH
import com.pkslow.ai.util.Constants.BARD_VERSION
import com.pkslow.ai.util.Constants.BASE_URL
import com.pkslow.ai.util.Constants.CONTENT_TYPE
import com.pkslow.ai.util.Constants.HOSTNAME
import com.pkslow.ai.util.Constants.TOKEN_COOKIE_NAME
import com.pkslow.ai.util.Constants.USER_AGENT
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody
import java.util.Objects
import java.util.concurrent.ThreadLocalRandom
import java.util.regex.Pattern

object BardUtils {
    fun createBuilderWithBardHeader(token: String): Request.Builder {
        return Request.Builder()
            .addHeader("Host", HOSTNAME)
            .addHeader("Content-Type", CONTENT_TYPE)
            .addHeader("X-Same-Domain", "1")
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Origin", BASE_URL)
            .addHeader("Referer", BASE_URL)
            .addHeader("Cookie", "$TOKEN_COOKIE_NAME=$token")
    }

    fun createRequestForSNlM0e(token: String): Request {
        val headerBuilder = createBuilderWithBardHeader(token)
        return headerBuilder.url(BASE_URL)
            .build()
    }

    fun fetchSNlM0eFromBody(input: String?): String? {
        val p = Pattern.compile("SNlM0e\":\"(.*?)\"")
        val m = p.matcher(input ?: "")
        if (m.find()) {
            var result = m.group()
            result = result.substring(9, result.length - 1)
            return result
        }
        return null
    }

    fun genQueryStringParamsForAsk(): Map<String, String> {
        var randonNum = ThreadLocalRandom.current().nextInt(0, 10000)
        randonNum = randonNum + 100000
        val params: MutableMap<String, String> = HashMap()
        params["bl"] = BARD_VERSION
        params["_reqid"] = randonNum.toString()
        params["rt"] = "c"
        return params
    }

    fun createHttpBuilderForAsk(): HttpUrl.Builder {
        val params = genQueryStringParamsForAsk()
        val httpBuilder =
            Objects.requireNonNull<HttpUrl?>((BASE_URL + ASK_QUESTION_PATH).toHttpUrlOrNull())
                .newBuilder()
        for ((key, value) in params) {
            httpBuilder.addQueryParameter(key, value)
        }
        return httpBuilder
    }

    /**
     * remove backslash \ in answer string
     */
    fun removeBackslash(answerStr: String): String {
        var answerStr = answerStr
        answerStr = answerStr.replace("\\\\n", "\n")
        answerStr = answerStr.replace("\\", "\"")
        return answerStr
    }

    fun createPostRequestForAsk(token: String, bardRequest: BardRequest): Request {
        val httpBuilder = createHttpBuilderForAsk()
        val body = buildRequestBodyForAsk(bardRequest)
        val headerBuilder = createBuilderWithBardHeader(token)
        return headerBuilder.url(httpBuilder.build())
            .method("POST", body)
            .build()
    }

    fun buildRequestBodyForAsk(bardRequest: BardRequest): RequestBody {
        return FormBody.Builder()
            .add(
                "f.req", String.format(
                    "[null,\"[[\\\"%s\\\"],null,[\\\"%s\\\",\\\"%s\\\",\\\"%s\\\"]]\"]",
                    bardRequest.question,
                    bardRequest.conversationId,
                    bardRequest.responseId,
                    bardRequest.choiceId
                )
            )
            .add("at", bardRequest.strSNlM0e)
            .build()
    }

    fun renderBardResponseFromResponse(content: String?): BardResponse {
        var conversationId: String? = ""
        var responseId: String? = ""
        var choiceId: String? = ""
        val answer = Answer()

        try {
            val jsonArray = Gson().fromJson(
                content,
                JsonArray::class.java
            )
            val element3 = (jsonArray[0] as JsonArray)[2]
            val content3 = element3.asString
            val jsonArray3 = Gson().fromJson(
                content3,
                JsonArray::class.java
            )
            conversationId = (jsonArray3[1] as JsonArray)[0].asString
            responseId = (jsonArray3[1] as JsonArray)[1].asString
            var chosenAnswer = (jsonArray3[0] as JsonArray)[0].asString
            chosenAnswer = removeBackslash(chosenAnswer)
            answer.chosenAnswer = chosenAnswer

            // somehow get the other drafts
            // ???
            choiceId = ((jsonArray3[4] as JsonArray)[0] as JsonArray)[0].asString
            try {
                val imageURL =
                    ((((((jsonArray3[4] as JsonArray)[0] as JsonArray)[4] as JsonArray)[0] as JsonArray)[3] as JsonArray)[0] as JsonArray)[0].asString
                val articleURL =
                    ((((((jsonArray3[4] as JsonArray)[0] as JsonArray)[4] as JsonArray)[0] as JsonArray)[1] as JsonArray)[0] as JsonArray)[0].asString
                answer.imageURL = imageURL
                answer.articleURL = articleURL
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
            answer.status = AnswerStatus.NO_ANSWER
            return BardResponse(conversationId!!, responseId!!, choiceId!!, answer)
        }
        answer.status = AnswerStatus.OK
        return BardResponse(conversationId, responseId, choiceId, answer)
    }

    fun isEmpty(str: String?): Boolean {
        return str == null || str.length == 0
    }
}