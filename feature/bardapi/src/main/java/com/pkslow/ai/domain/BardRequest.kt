/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package com.pkslow.ai.domain

import com.pkslow.ai.util.Constants.EMPTY_STRING
import java.util.Objects

class BardRequest(
    var strSNlM0e: String,
    var question: String,
    var conversationId: String,
    var responseId: String,
    var choiceId: String
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as BardRequest
        return strSNlM0e == that.strSNlM0e && question == that.question && conversationId == that.conversationId && responseId == that.responseId && choiceId == that.choiceId
    }

    override fun hashCode(): Int {
        return Objects.hash(strSNlM0e, question, conversationId, responseId, choiceId)
    }

    override fun toString(): String {
        return "BardRequest{" +
                "strSNlM0e=" + strSNlM0e +
                ", question=" + question +
                ", conversationId=" + conversationId +
                ", responseId=" + responseId +
                ", choiceId=" + choiceId +
                '}'
    }

    companion object {
        fun newEmptyBardRequest(): BardRequest {
            return BardRequest(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING)
        }
    }
}