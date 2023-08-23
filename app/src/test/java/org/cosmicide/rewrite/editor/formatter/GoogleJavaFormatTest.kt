/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.editor.formatter

import org.cosmicide.editor.formatter.GoogleJavaFormat
import org.hamcrest.MatcherAssert
import org.junit.Test

class GoogleJavaFormatTest {

    @Test
    fun `should format code with default options`() {
        val input = "public class MyClass { }"
        val expectedOutput = "public class MyClass {}\n"
        val actualOutput = GoogleJavaFormat.formatCode(input)
        MatcherAssert.assertThat(expectedOutput, org.hamcrest.CoreMatchers.`is`(actualOutput))
    }

    @Test
    fun `should format code with AOSP style`() {
        val input = "public class MyClass { }"
        val expectedOutput = "public class MyClass {\n}\n"
        val actualOutput = GoogleJavaFormat.formatCode(input)
        MatcherAssert.assertThat(expectedOutput, org.hamcrest.CoreMatchers.`is`(actualOutput))
    }

    @Test
    fun `should format code with custom options`() {
        val input = "public class MyClass { }"
        val expectedOutput = "public class MyClass {}\n"
        val actualOutput = GoogleJavaFormat.formatCode(input)
        MatcherAssert.assertThat(expectedOutput, org.hamcrest.CoreMatchers.`is`(actualOutput))
    }

    @Test
    fun `should handle empty input`() {
        val input = ""
        val expectedOutput = ""
        val actualOutput = GoogleJavaFormat.formatCode(input)
        MatcherAssert.assertThat(expectedOutput, org.hamcrest.CoreMatchers.`is`(actualOutput))
    }
}