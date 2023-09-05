/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */
package com.pkslow.ai.domain

class Answer(
    var status: AnswerStatus = AnswerStatus.NO_ANSWER,
    var chosenAnswer: String = "",
    var images: List<Image> = listOf(),
) {
    fun markdown(): String {
        var markdown = this.chosenAnswer

        if (images.isNotEmpty()) {
            for (image in images) {
                markdown = markdown.replaceFirst(image.labelRegex(), image.markdown())
            }
        }
        return markdown
    }
}


enum class AnswerStatus {
    OK,
    NO_ANSWER,
    ERROR
}