package org.cosmic.ide

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmic.ide.analyzer.java.JavacAnalyzer
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.project.JavaProject
import java.io.File

class ProblemMarker(
    private val context: Context,
    private val editor: CodeEditor,
    private val file: File,
    private val project: JavaProject
) : ContentListener {

    private val analyzer: JavacAnalyzer by lazy { JavacAnalyzer(context, project) }
    private val diagnostics = DiagnosticsContainer()
    private val TAG = "ProblemMarker"

    init {
        analyze(editor.text)
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

    private fun analyze(content: Content) {
        val code = content.toString()
        FileUtil.writeFile(file.absolutePath, code)
        CoroutineUtil.inParallel {
            if (!analyzer.isFirstRun()) {
                analyzer.reset()
            }
            try {
                if (!(file.extension.equals("java"))) {
                    Handler(Looper.getMainLooper()).post {
                        editor.diagnostics = DiagnosticsContainer()
                    }
                    return@inParallel
                }
                analyzer.analyze()
                diagnostics.reset()
                diagnostics.addDiagnostics(analyzer.getDiagnostics())
                Handler(Looper.getMainLooper()).post {
                    editor.diagnostics = diagnostics
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error while marking diagnostics.", e)
            }
        }
    }
}
