/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.chat

import org.cosmicide.chat.ChatProvider
import org.cosmicide.fragment.Models
import org.junit.Test

class ChatProviderTest {
    @Test
    fun `should generate chat`() {
        val conversation = listOf(
            mapOf("author" to "user", "text" to "hi"),
            mapOf("author" to "bot", "text" to "hello")
        )
        val models = Models.values().map { it.name }
        for (model in models) {
            val output = ChatProvider.generate(model, conversation)
            println(output)
        }
    }
}