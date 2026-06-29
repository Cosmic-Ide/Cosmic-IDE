/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers

import android.util.Log
import com.sun.tools.javac.api.JavacTool
import com.sun.tools.javac.file.JavacFileManager
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.Quickfix
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmicide.common.Prefs
import org.cosmicide.completion.java.parser.CompletionProvider
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.FileUtil
import java.io.File
import java.nio.charset.Charset
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

class JavaAnalyzer(
    val editor: CodeEditor,
    val project: Project,
    val compilerOptions: List<String> = mutableListOf()
) : AutoCloseable {
    private val args by lazy {
        listOf(
            "-XDstringConcat=inline",
            "-XDcompilePolicy=byfile",
            "-XD-Xprefer=source",
            "-XDide",
            "-XDev",
            "-XDsuppressAbortOnBadClassFile",
            "-XDshould-stop.at=GENERATE",
            "-XDdiags.formatterOptions=-source",
            "-XDdiags.layout=%L%m|%L%m|%L%m",
            "-XDbreakDocCommentParsingOnError=false",
            "-XDallowStringFolding=false",
            "-XDshould-stop.at=FLOW",
            "-Xlint:cast",
            "-Xlint:deprecation",
            "-Xlint:empty",
            "-Xlint:fallthrough",
            "-Xlint:finally",
            "-Xlint:path",
            "-Xlint:unchecked",
            "-Xlint:varargs",
            "-Xlint:static",
            "-proc:none"
        )
    }
    private var diagnostics = DiagnosticCollector<JavaFileObject>()
    private val tool: JavacTool by lazy { JavacTool.create() }
    private val standardFileManager: JavacFileManager by lazy {
        tool.getStandardFileManager(
            diagnostics, Locale.getDefault(), Charset.defaultCharset()
        )
    }
    private var lastClasspath: List<File>? = null

    init {
        standardFileManager.setLocation(
            StandardLocation.PLATFORM_CLASS_PATH, FileUtil.classpathDir.walk().toList()
        )
        if (!project.binDir.exists()) {
            project.binDir.mkdirs()
        }
        standardFileManager.setLocation(StandardLocation.CLASS_OUTPUT, listOf(project.binDir))
    }

    fun analyze() {
        val version = Prefs.compilerJavaVersion
        val toCompile = getSourceFiles()
        if (toCompile.isEmpty()) return

        with(standardFileManager) {
            updateClasspathIfNeeded()
            autoClose = false
        }

        val copy = args.toMutableList()
        copy.apply {
            add("--release")
            add(version.toString())
            addAll(compilerOptions)
        }

        try {
        tool.getTask(System.out.writer(), standardFileManager, diagnostics, copy, null, toCompile)
            .apply {
                parse()
                analyze()
                if (diagnostics.diagnostics.isEmpty()) {
                    try {
                        generate().forEach(Cache::saveCache)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("JavaAnalyzer", "Error during parsing", e)
        }
    }


    fun reset() {
        diagnostics = DiagnosticCollector<JavaFileObject>()
    }

    fun getDiagnostics(): List<DiagnosticRegion> {
        val diagnostic = diagnostics.diagnostics
        val problems = mutableListOf<DiagnosticRegion>()
        try {
            for (it in diagnostic) {
                if (it.source == null) continue
                val severity =
                    if (it.kind == Diagnostic.Kind.ERROR) DiagnosticRegion.SEVERITY_ERROR else DiagnosticRegion.SEVERITY_WARNING

                val message = it.getMessage(Locale.getDefault())
                val quickFixes = mutableListOf<Quickfix>()
                if (it.code == "compiler.err.cant.resolve.location") {
                    val symbol = it.source.getCharContent(true)
                        .substring(it.startPosition.toInt(), it.endPosition.toInt())
                    CompletionProvider.symbolCacher.filterClassNames(symbol).forEach { name ->
                        quickFixes.add(Quickfix("Import ${name.value}", 0L) {
                            val lines = editor.text.lines()
                            var firstImportLine = lines.indexOfFirst { it.startsWith("import ") }
                            if (firstImportLine == -1) {
                                firstImportLine =
                                    lines.indexOfFirst { it.startsWith("package ") } + 1
                            }
                            editor.text.insert(
                                firstImportLine, 0, "import ${name.key}.${name.value};\n"
                            )
                        })
                    }
                }

                problems.add(
                    DiagnosticRegion(
                        it.startPosition.toInt(),
                        it.endPosition.toInt(),
                        severity,
                        0,
                        DiagnosticDetail(message, quickfixes = quickFixes),
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return problems
    }

    private fun getClasspath(): List<File> {
        val classpath = mutableListOf<File>()
        classpath.add(File(project.binDir, "classes"))
        project.libDir.walk().forEach {
            if (it.extension == "jar") {
                classpath.add(it)
            }
        }


        project.binDir.resolve("classes").walk().filter { it.extension == "class" }.forEach {
            if (Cache.getCache(it) != null && Cache.getCache(it)!!.lastModified == it.lastModified()) {
                classpath.add(it)
            }
        }

        return classpath
    }

    private fun updateClasspathIfNeeded() {
        val currentClasspath = getClasspath()
        if (currentClasspath != lastClasspath) {
            standardFileManager.setLocation(StandardLocation.CLASS_PATH, currentClasspath)
            lastClasspath = currentClasspath
        }
    }

    private fun getSourceFiles(): List<JavaFileObject> {
        val allFiles = project.srcDir.walk().filter { it.extension == "java" }.toList()
        val dirtyFiles = allFiles.filter { file ->
            val cache = Cache.getCache(file)
            cache == null || cache.lastModified < file.lastModified()
        }

        // If any file changed, recompile everything to ensure stability
        return if (dirtyFiles.isNotEmpty()) {
            allFiles.map { Cache.saveCache(it) }
        } else {
            emptyList()
        }
    }

    override fun close() {
        try {
            standardFileManager.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
