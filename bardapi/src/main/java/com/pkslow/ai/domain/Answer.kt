/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package com.pkslow.ai.domain

import com.pkslow.ai.util.BardUtils

class Answer(
    var status: AnswerStatus = AnswerStatus.NO_ANSWER,
    var chosenAnswer: String? = null,
    var imageURL: String? = null,
    var articleURL: String? = null,
) {
    val answers = mutableListOf<String>()
    fun markdown(): String {
        val sb = StringBuilder()
        sb.append(chosenAnswer)
        if (!BardUtils.isEmpty(imageURL)) {
            sb.append("\n\n")
            sb.append("[![](")
            sb.append(imageURL)
            sb.append(")](")
            sb.append(articleURL)
            sb.append(")")
        }
        return sb.toString()
    }
}


enum class AnswerStatus {
    OK,
    NO_ANSWER,
    ERROR
}