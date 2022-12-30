package org.cosmic.ide.analyzer.java

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.sun.source.util.JavacTask
import com.sun.tools.javac.api.JavacTool
import com.sun.tools.javac.file.JavacFileManager
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import org.cosmic.ide.CompilerUtil
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.project.JavaProject
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation

class JavacAnalyzer(
    context: Context,
    private val project: JavaProject
) {

    private val prefs: SharedPreferences
    private var diagnostics = DiagnosticCollector<JavaFileObject>()
    private val tool: JavacTool by lazy { JavacTool.create() }
    private val standardFileManager: JavacFileManager by lazy {
        tool.getStandardFileManager(
            diagnostics, Locale.getDefault(), Charset.defaultCharset()
        )
    }
    private var isFirstUse = true
    private val TAG = "JavacAnalyzer"

    init {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        standardFileManager.setLocation(
            StandardLocation.PLATFORM_CLASS_PATH, CompilerUtil.platformClasspath
        )
    }

    @Throws(IOException::class)
    fun analyze() {
        val version = prefs.getString("java_version", "7")
        val files = getSourceFiles(File(project.srcDirPath))

        val javaFileObjects = mutableListOf<JavaFileObject>()
        for (file in files) {
            javaFileObjects.add(
                object : SimpleJavaFileObject(
                    file.toURI(), JavaFileObject.Kind.SOURCE
                ) {
                    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                        return FileUtil.readFile(file)
                    }
                })
        }

        with(standardFileManager) {
            setLocation(StandardLocation.CLASS_PATH, getClasspath())
            setLocation(
                StandardLocation.SOURCE_PATH, files
            )
        }

        val args = mutableListOf<String>()

        args.add("-proc:none")
        args.add("-source")
        args.add(version!!)
        args.add("-target")
        args.add(version)

        val task =
            tool.getTask(
                null,
                standardFileManager,
                diagnostics,
                args,
                null,
                javaFileObjects
            ) as JavacTask

        task.parse()
        task.analyze()
        standardFileManager.close()
        isFirstUse = false
    }

    fun isFirstRun() = isFirstUse

    fun reset() {
        diagnostics = DiagnosticCollector<JavaFileObject>()
    }

    fun getDiagnostics(): List<DiagnosticRegion> {
        val diagnostic = diagnostics.diagnostics
        val problems = mutableListOf<DiagnosticRegion>()
        for (it in diagnostic) {
            if (it.source == null) continue
            val severity = if (it.kind == Diagnostic.Kind.ERROR) DiagnosticRegion.SEVERITY_ERROR else DiagnosticRegion.SEVERITY_WARNING
            problems.add(
                DiagnosticRegion(
                    it.startPosition.toInt(), it.endPosition.toInt(), severity, 0, DiagnosticDetail(it.getMessage(Locale.getDefault()))
                )
            )
        }
        return problems
    }

    private fun getClasspath(): List<File> {
        val classpath = mutableListOf<File>()
        classpath.add(File(project.binDirPath, "classes"))
        File(project.libDirPath).walk().forEach {
            if (it.extension.equals("jar")) {
                classpath.add(it)
            }
        }

        return classpath
    }

    private fun getSourceFiles(path: File): List<File> {
        val sourceFiles = mutableListOf<File>()

        path.walk().forEach {
            if (it.extension.equals("java")) {
                sourceFiles.add(it)
            }
        }

        return sourceFiles
    }
}
