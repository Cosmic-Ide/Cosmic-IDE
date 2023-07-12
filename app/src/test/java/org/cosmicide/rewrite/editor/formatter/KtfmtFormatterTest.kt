/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.editor.formatter

import org.junit.Test

class ktfmtFormatterTest {

    @Test
    fun `should format code with default options`() {
        val code = """
            fun main() {
                val foo = "bar"
            }
        """.trimIndent()
        val formattedCode = ktfmtFormatter.formatCode(code)
        assert("fun main() {\n    val foo = \"bar\"\n}\n" == formattedCode)
    }

    @Test
    fun `should not modify already formatted code`() {
        val code = "fun main() {\n    val foo = \"bar\"\n}\n"
        val formattedCode = ktfmtFormatter.formatCode(code)
        assert(code == formattedCode)
    }

    @Test
    fun `should handle code with syntax errors`() {
        val code = "foo bar"
        val formattedCode = ktfmtFormatter.formatCode(code)
        assert(code == formattedCode)
    }

    @Test
    fun `should handle empty input`() {
        val input = ""
        val formattedCode = ktfmtFormatter.formatCode(input)
        assert("" == formattedCode)
    }
}