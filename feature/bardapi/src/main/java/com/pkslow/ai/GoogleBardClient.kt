/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package com.pkslow.ai

import com.pkslow.ai.domain.Answer
import com.pkslow.ai.domain.AnswerStatus
import com.pkslow.ai.domain.BardRequest
import com.pkslow.ai.domain.BardResponse
import com.pkslow.ai.utils.BardUtils.createPostRequestForAsk
import com.pkslow.ai.utils.BardUtils.createRequestForSNlM0e
import com.pkslow.ai.utils.BardUtils.fetchSNlM0eFromBody
import com.pkslow.ai.utils.BardUtils.renderBardResponseFromResponse
import com.pkslow.ai.utils.Constants.EMPTY_STRING
import com.pkslow.ai.utils.WebUtils.okHttpClientWithTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration
import java.util.Objects

class GoogleBardClient : AIClient {
    private val token: String
    private val httpClient: OkHttpClient
    private val bardRequest = BardRequest.newEmptyBardRequest()

    constructor(token: String, timeout: Duration = Duration.ofMinutes(5)) {
        this.token = token
        httpClient = okHttpClientWithTimeout(timeout)
    }

    constructor(token: String, httpClient: OkHttpClient) {
        this.token = token
        this.httpClient = httpClient
    }

    override fun ask(question: String): Answer {
        val answer: Answer
        try {
            if (bardRequest.strSNlM0e.isEmpty()) {
                bardRequest.strSNlM0e = callBardToGetSNlM0e()
            }
            bardRequest.question = question
            val response = callBardToAsk(bardRequest)
            answer = processAskResult(response)
        } catch (e: Throwable) {
            return Answer(AnswerStatus.ERROR)
        }
        return answer
    }

    override fun reset() {
        bardRequest.strSNlM0e = EMPTY_STRING
        bardRequest.conversationId = EMPTY_STRING
        bardRequest.responseId = EMPTY_STRING
        bardRequest.choiceId = EMPTY_STRING
    }

    private fun callBardToGetSNlM0e(): String {
        val call = httpClient.newCall(createRequestForSNlM0e(token))
        try {
            call.execute().use { response ->
                val responseString =
                    response.body.string()
                return fetchSNlM0eFromBody(responseString) ?: throw RuntimeException(
                    "Can't get SNlM0e"
                )
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun callBardToAsk(bardRequest: BardRequest): String {
        val request: Request = createPostRequestForAsk(token, bardRequest)
        val call = httpClient.newCall(request)
        try {
            call.execute().use { response ->
                val statusCode = response.code
                val responseString =
                    Objects.requireNonNull(response.body).string()
                check(statusCode == 200) { "Can't get the answer" }
                return responseString.split("\\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray<String>()[3]
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun processAskResult(content: String): Answer {
        val bardResponse: BardResponse = renderBardResponseFromResponse(content)
        bardRequest.conversationId = bardResponse.conversationId
        bardRequest.responseId = bardResponse.responseId
        bardRequest.choiceId = bardResponse.choiceId
        return bardResponse.answer
    }
}