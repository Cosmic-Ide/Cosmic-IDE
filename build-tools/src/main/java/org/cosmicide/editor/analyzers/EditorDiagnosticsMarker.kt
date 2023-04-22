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
import org.jetbrains.kotlin.backend.wasm.ir2wasm.bind
import java.io.File

class EditorDiagnosticsMarker(
    private val editor: CodeEditor,
    private val file: File,
    private val project: Project
) : ContentListener {

    private val analyzer: JavaAnalyzer by lazy { JavaAnalyzer(editor, project, file) }
    private val diagnostics = DiagnosticsContainer()

    init {
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
}