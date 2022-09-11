package org.cosmic.ide

import android.content.Context
import android.os.Looper
import android.os.Handler
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmic.ide.analyzer.java.JavacAnalyzer
import org.cosmic.ide.common.Indexer
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.project.JavaProject
import java.io.File

class ProblemMarker(
    context: Context,
    editor: CodeEditor,
    file: File,
    project: JavaProject
) : ContentListener {

    private var editor: CodeEditor
    private var analyzer: JavacAnalyzer
    private var file: File
    private var project: JavaProject
    private val diagnostics = DiagnosticsContainer()

    init {
        this.editor = editor
        this.file = file
        this.project = project
        this.analyzer = JavacAnalyzer(context, project)
        analyze(editor.getText())
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

    private fun analyze(content: Content) {
        val code = content.toString()
        FileUtil.writeFile(file.getAbsolutePath(), code)
        CoroutineUtil.inParallel thread@ {
            if (!analyzer.isFirstRun()) {
                analyzer.reset()
            }
            try {
                if (!(file.extension.equals(".java") || file.extension.equals(".jav"))) {
                    Handler(Looper.getMainLooper()).post {
                        editor.setDiagnostics(DiagnosticsContainer())
                    }
                    return@thread
                }
                analyzer.analyze()
                diagnostics.reset()
                diagnostics.addDiagnostics(analyzer.getDiagnostics())
                Handler(Looper.getMainLooper()).post {
                    editor.setDiagnostics(diagnostics)
                }
            } catch (ignored: Exception) {
            }
        }
    }
}
