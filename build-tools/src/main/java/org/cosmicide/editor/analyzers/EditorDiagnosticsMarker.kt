/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers

import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.project.Project
import java.io.File

class EditorDiagnosticsMarker : ContentListener {

    private val analyzer: JavaAnalyzer by lazy { JavaAnalyzer(editor, project, file) }
    private val diagnostics = DiagnosticsContainer()
    private lateinit var editor: CodeEditor
    private lateinit var file: File
    private lateinit var project: Project

    fun init(editor: CodeEditor, file: File, project: Project) {
        this.editor = editor
        this.file = file
        this.project = project
        analyze(editor.text)
        editor.post {
            val window = editor.getComponent(EditorDiagnosticTooltipWindow::class.java)
            window.setSize(500, 100)
            window.parentView.setBackgroundColor(editor.colorScheme.getColor(EditorColorScheme.WHOLE_BACKGROUND))
        }
    }

    override fun beforeReplace(content: Content) {}

    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        analyze(content)
    }

    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        analyze(content)
    }

    private fun analyze(content: Content) = CoroutineScope(Dispatchers.IO).launch {
        val code = content.toString()
        file.writeText(code)
        analyzer.reset()
        try {
            if (file.extension != "java") {
                return@launch
            }
            analyzer.analyze()
            diagnostics.reset()
            diagnostics.addDiagnostics(analyzer.getDiagnostics())
            editor.post {
                editor.diagnostics = diagnostics
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val INSTANCE = EditorDiagnosticsMarker()
    }
}