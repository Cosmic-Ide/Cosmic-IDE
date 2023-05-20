package org.cosmicide.build.java

import com.sun.tools.javac.api.JavacTool
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.project.Project
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.util.FileUtil
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
        val output = File(project.binDir, "classes")
        val version = Prefs.compilerJavaVersion.toString()

        try {
            Files.createDirectories(output.toPath())
        } catch (e: Exception) {
            reporter.reportWarning(e.stackTraceToString())
        }

        val javaFiles = getSourceFiles(project.srcDir)

        if (javaFiles.isEmpty()) {
            reporter.reportInfo("No java files found. Skipping compilation.")
            return
        }

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

            val options = listOf("-proc:none", "-release", version)

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
                    Diagnostic.Kind.NOTE, Diagnostic.Kind.WARNING, Diagnostic.Kind.MANDATORY_WARNING -> reporter.reportWarning(message.toString())
                    else -> reporter.reportInfo(message.toString())
                }
            }
        }
    }

    fun getSourceFiles(directory: File): List<File> {
        return directory.listFiles()?.filter {
            it.isFile && it.extension == "java"
        } ?: emptyList()
    }

    fun getClasspath(project: Project): List<File> {
        val classpath = mutableListOf(File(project.binDir, "classes"))
        val libDir = project.libDir
        if (libDir.exists() && libDir.isDirectory) {
            classpath += libDir.listFiles()?.toList() ?: emptyList()
        }
        return classpath
    }

    fun getSystemClasspath(): List<File> {
        return FileUtil.classpathDir.listFiles()?.toList() ?: emptyList()
    }
}