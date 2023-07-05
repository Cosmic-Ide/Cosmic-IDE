/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.editor.formatter

import com.google.googlejavaformat.java.Main
import org.cosmicide.rewrite.common.Prefs
import java.io.OutputStreamWriter
import java.io.PrintWriter
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object GoogleJavaFormat {
    fun formatCode(code: String): String {
        val file = createTempFile("file", ".java")
        file.writeText(code)
        println("Formatting code...")

        val args = mutableListOf<String>().apply {
            addAll(Prefs.googleJavaFormatOptions ?: emptyList())
            if (Prefs.googleJavaFormatStyle == "aosp") {
                add("--aosp")
            }
            add("--replace")
            add(file.toAbsolutePath().toString())
        }

        Main(
            PrintWriter(OutputStreamWriter(System.out)),
            PrintWriter(OutputStreamWriter(System.err)),
            System.`in`
        ).format(*args.toTypedArray())

        val formattedCode = file.readText()
        file.deleteIfExists()

        return formattedCode
    }
}