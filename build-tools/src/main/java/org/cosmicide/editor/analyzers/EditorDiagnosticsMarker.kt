/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers

import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.common.Prefs
import org.cosmicide.project.Project
import java.io.File

class EditorDiagnosticsMarker(
    val editor: CodeEditor,
    val file: File,
    val project: Project
) : EventReceiver<ContentChangeEvent> {

    private val diagnostics = DiagnosticsContainer()
    private var analyzer = JavaAnalyzer(
        editor,
        project,
        if (Prefs.javacFlags.isNotEmpty()) Prefs.javacFlags.split(" ").toList() else listOf()
    )

    init {
        analyze(editor.text)
    }

    override fun onReceive(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
        analyze(event.editor.text)
    }

    private fun analyze(content: Content) = CoroutineScope(Dispatchers.IO).launch {
        file.writeText(content.toString())
        analyzer.reset()

        analyzer.analyze()
        diagnostics.reset()
        diagnostics.addDiagnostics(analyzer.getDiagnostics())

        editor.diagnostics = diagnostics
    }
}
