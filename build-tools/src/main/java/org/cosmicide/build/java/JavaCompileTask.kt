/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.build.java

import com.sun.tools.javac.api.JavacTool
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.build.util.getSourceFiles
import org.cosmicide.build.util.getSystemClasspath
import org.cosmicide.common.Prefs
import org.cosmicide.project.Project
import java.io.File
import java.io.Writer
import java.nio.file.Files
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation

class JavaCompileTask(val project: Project) : Task {
    val diagnostics = DiagnosticCollector<JavaFileObject>()
    val tool = JavacTool.create()
    val fileManager = tool.getStandardFileManager(diagnostics, null, null)

    override fun execute(reporter: BuildReporter) {
        val output = project.binDir.resolve("classes")
        val version = Prefs.compilerJavaVersion.toString()

        try {
            Files.createDirectories(output.toPath())
        } catch (e: Exception) {
            reporter.reportWarning(e.stackTraceToString())
        }

        val javaFiles = project.srcDir.getSourceFiles("java")

        if (javaFiles.isEmpty()) {
            reporter.reportInfo("No java files found. Skipping compilation.")
            return
        }

        reporter.reportInfo("Compilingg")

        val size = javaFiles.size
        reporter.reportInfo("Compiling $size java ${if (size == 1) "file" else "files"}...")

        val javaFileObjects = javaFiles.map { file ->
            object : SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
                override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                    return file.readText()
                }
            }
        }

        fileManager.use { fm ->
            fm.setLocation(StandardLocation.CLASS_OUTPUT, listOf(output))
            fm.setLocation(StandardLocation.PLATFORM_CLASS_PATH, getSystemClasspath())
            fm.setLocation(StandardLocation.CLASS_PATH, getClasspath(project))
            fm.setLocation(StandardLocation.SOURCE_PATH, javaFiles)
            
            val flags = Prefs.javacFlags

            val options = listOf(
                "-XDstringConcat=inline",
                "-proc:none",
                "-source",
                version,
                "-target",
                version
            ) + if (flags.isNotEmpty()) flags.split(" ").toList() else listOf()

            val task = tool.getTask(
                object : Writer() {
                    private val sb = StringBuilder()
                    override fun close() = flush()
                    override fun flush() {
                        reporter.reportInfo(sb.toString())
                        sb.clear()
                    }

                    override fun write(cbuf: CharArray?, off: Int, len: Int) {
                        sb.appendRange(cbuf!!, off, off + len)
                        reporter.reportInfo(sb.toString())
                    }
                },
                fm,
                diagnostics,
                options,
                null,
                javaFileObjects
            )

            task.call()

            for (diagnostic in diagnostics.diagnostics) {
                val message = StringBuilder()
                diagnostic.source?.apply {
                    message.append("$name:${diagnostic.lineNumber}: ")
                }
                message.append(diagnostic.getMessage(Locale.getDefault()))

                when (diagnostic.kind) {
                    Diagnostic.Kind.ERROR, Diagnostic.Kind.OTHER -> reporter.reportError(message.toString())
                    Diagnostic.Kind.NOTE, Diagnostic.Kind.WARNING, Diagnostic.Kind.MANDATORY_WARNING -> reporter.reportWarning(
                        message.toString()
                    )

                    else -> reporter.reportInfo(message.toString())
                }
            }
        }
    }

    fun getClasspath(project: Project): List<File> {
        val classpath = mutableListOf(File(project.binDir, "classes"))
        val libDir = project.libDir
        if (libDir.exists() && libDir.isDirectory) {
            classpath += libDir.walk().filter { it.extension == "jar" }.toList()
        }
        return classpath
    }
}
