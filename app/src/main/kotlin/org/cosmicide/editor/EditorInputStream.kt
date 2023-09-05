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

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor

import android.util.Log
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.InputStream


class EditorInputStream(private val editor: CodeEditor) : InputStream() {
    private val lineBuffer = StringBuilder()

    override fun read(): Int {
        if (lineBuffer.isEmpty()) {
            Log.d("EditorInputStream", "lineBuffer is empty")
            readLineToBuffer()
        }
        if (lineBuffer.isEmpty()) {
            return -1
        }
        if (lineBuffer.isBlank()) {
            return -1
        }
        Log.d("lineBuffer", "${lineBuffer.length}")
        val code = lineBuffer[0].code
        lineBuffer.deleteCharAt(0)
        return code
    }

    private fun readLineToBuffer() {
        while (true) {
            val lines = editor.text.lines()
            if (lines.isEmpty()) {
                continue
            }
            if (lines.size == 1) {
                continue
            }
            val line = lines[lines.size - 1]
            Log.d("EditorInputStream", line)
            if (line.isBlank()) {
                lineBuffer.append(lines[lines.size - 2]).append('\n')
                Log.d("buffer", lineBuffer.toString())
                break
            }
        }
    }
}
