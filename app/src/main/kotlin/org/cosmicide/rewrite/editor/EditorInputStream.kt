/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.editor

import android.util.Log
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.InputStream


class EditorInputStream(private val editor: CodeEditor) : InputStream() {
    private val lineBuffer = StringBuilder()

    override fun read(): Int {
        if (lineBuffer.isEmpty()) {
            readLineToBuffer()
        }
        if (lineBuffer.isEmpty()) {
            return -1
        }
        val c = lineBuffer[0]
        lineBuffer.deleteCharAt(0)
        return c.code
    }

    private fun readLineToBuffer() {
        var lineComplete = false
        while (!lineComplete) {
            lineComplete = try {
                val line = editor.text.getLineString(editor.lineCount - 1)
                if (line.isEmpty()) {
                    val data = editor.text.getLineString(editor.lineCount - 2)
                    lineBuffer.append(data).append('\n')
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("EditorInputStream", "Error reading line", e)
                false
            }
        }
    }
}
