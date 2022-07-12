package com.pranav

import com.pranav.analyzer.java.JavacAnalyzer
import com.pranav.common.Indexer
import com.pranav.common.util.CoroutineUtil
import com.pranav.common.util.FileUtil
import com.pranav.project.mode.JavaProject

import io.github.rosemoe.sora.lang.diagnostic.*
import io.github.rosemoe.sora.text.*
import io.github.rosemoe.sora.widget.CodeEditor

class ProblemMarker(
        editor: CodeEditor,
        file: String,
        project: JavaProject
) : ContentListener {

    private var editor: CodeEditor
    private var analyzer: JavacAnalyzer
    private var project: JavaProject
    private val DiagnosticsContainer diagnostics = DiagnosticsContainer()

    init {
        this.editor = editor
        this.project = project
        this.analyzer = JavacAnalyzer(editor.getContext(), file, project)
        run(editor.getText())
    }

    override fun beforeReplace(content: Content) {
        
    }

    override fun afterInsert(
            content: Content,
            startLine: Int,
            startColumn: Int,
            endLine: Int,
            endColumn: Int,
            insertedContent: CharSequence
    ) {
        run(content)
    }

    override fun afterDelete(
            content: Content,
            startLine: Int,
            startColumn: Int,
            endLine: Int,
            endColumn: Int,
            deletedContent: CharSequence
    ) {
        run(content)
    }

    private fun run(content: Content) {
        CoroutineUtil.inParallel {
            if (!analyzer.isFirstRun()) {
                analyzer.reset()
            }
            try {
                val path = Indexer(project.getProjectName(), project.getCacheDirPath()).getString("currentFile")
                FileUtil.writeFile(path, content.toString())
                analyzer.analyze()
            } catch (ignored: Exception) {
                
            }
            diagnostics.reset()
            diagnostics.addDiagnostics(analyzer.getDiagnostics())
            editor.setDiagnostics(diagnostics)
        }
    }
}
