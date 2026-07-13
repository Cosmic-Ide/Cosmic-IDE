/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.ide.editor

import io.github.rosemoe.sora.text.TextRange
import org.cosmicide.plugin.api.ConfigurableExtension
import org.cosmicide.project.Project
import java.io.File

data class EditorFormatterRequest(
    val project: Project?,
    val file: File,
    val text: String,
    val range: TextRange? = null
)

data class EditorFormatterResult(
    val text: String,
    val replacementRange: TextRange? = null
)

interface EditorFormatterProvider : ConfigurableExtension {
    val priority: Int
        get() = 0

    fun supports(request: EditorFormatterRequest): Boolean

    fun format(request: EditorFormatterRequest): EditorFormatterResult
}
