/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.formatter

import com.facebook.ktfmt.cli.Main
import org.cosmicide.common.Prefs
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ktfmtFormatter {
    fun formatCode(code: String): String {
        val file = createTempFile("file", ".kt").apply { writeText(code) }
        val args = listOf("--style", Prefs.ktfmtStyle, file.toAbsolutePath().toString())

        Main(System.`in`, System.out, System.err, args.toTypedArray()).run()
        val formattedCode = file.readText()
        file.deleteIfExists()

        return formattedCode
    }
}
