package org.cosmicide.build.java

import com.sun.tools.javac.api.JavacTool
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.FileUtil
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.util.Collections
import java.util.Locale
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.MANDATORY_WARNING
import javax.tools.Diagnostic.Kind.NOTE
import javax.tools.Diagnostic.Kind.OTHER
import javax.tools.Diagnostic.Kind.WARNING
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation

class JavaCompileTask(val project: Project) : Task {

    private val tool by lazy {
        JavacTool.create()
    }

    override fun execute(reporter: BuildReporter) {
        val output = File(project.binDir, "classes")
        val version = "8"
        println("Current Java Version: $version")

        val diagnostics = DiagnosticCollector<JavaFileObject>()

        if (!output.exists()) {
            output.mkdirs()
        }
        val javaFileObjects = mutableListOf<SimpleJavaFileObject>()
        val javaFiles = getSourceFiles(project.srcDir.invoke())
        for (file in javaFiles) {
            val path = file.absolutePath
            File(output, path.replaceFirst(project.srcDir.invoke().absolutePath, "")).delete()
            javaFileObjects.add(
                object : SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
                    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                        return file.readText()
                    }
                })
        }

        if (javaFileObjects.isEmpty()) {
            return
        }

        val standardJavaFileManager =
            tool.getStandardFileManager(
                diagnostics, Locale.getDefault(), Charset.defaultCharset()
            )
        standardJavaFileManager.setLocation(
            StandardLocation.CLASS_OUTPUT, Collections.singletonList(output)
        )
        standardJavaFileManager.setLocation(
            StandardLocation.PLATFORM_CLASS_PATH, getSystemClasspath()
        )
        standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, getClasspath(project))
        standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, javaFiles)

        val args = arrayListOf<String>()

        args.add("-proc:none")
        args.add("-Werror")
        args.add("-source")
        args.add(version)
        args.add("-target")
        args.add(version)

        val task =
            tool.getTask(
                object : Writer() {
                    val sb = StringBuilder()
                    override fun close() {
                        flush()
                    }

                    override fun flush() {
                        reporter.reportInfo(sb.toString())
                        sb.clear()
                    }

                    override fun write(cbuf: CharArray?, off: Int, len: Int) {
                        sb.appendRange(cbuf!!, off, off + len)
                        reporter.reportInfo(sb.toString())
                    }

                },
                standardJavaFileManager,
                diagnostics,
                args,
                null,
                javaFileObjects
            )

        task.call()
        for (diagnostic in diagnostics.diagnostics) {
            val message = StringBuilder()
            if (diagnostic.source != null) {
                message.append(diagnostic.source.name)
                message.append(":")
                message.append(diagnostic.lineNumber)
                message.append(": ")
            }
            message.append(diagnostic.kind.name)
            message.append(": ")
            message.append(diagnostic.getMessage(Locale.getDefault()))

            when (diagnostic.kind) {
                ERROR, OTHER -> {
                    reporter.reportError(message.toString())
                }

                NOTE, WARNING, MANDATORY_WARNING -> {
                    reporter.reportWarning(message.toString())
                }

                else -> reporter.reportInfo(message.toString())
            }
        }

    }

    private fun getSourceFiles(path: File): List<File> {
        val sourceFiles = arrayListOf<File>()
        val files = path.listFiles() ?: return sourceFiles
        for (file in files) {
            if (file.isFile) {
                if (file.extension == "java") {
                    sourceFiles.add(file)
                }
            } else {
                sourceFiles.addAll(getSourceFiles(file))
            }
        }
        return sourceFiles
    }

    private fun getClasspath(project: Project): List<File> {
        val classpath = arrayListOf<File>()

        classpath.add(File(project.binDir, "classes"))
        val libs = project.libDir.listFiles()
        if (libs != null) {
            classpath.addAll(libs)
        }
        return classpath
    }

    private fun getSystemClasspath(): List<File> {
        val classpath = arrayListOf<File>()
        FileUtil.classpathDir.listFiles()?.forEach { classpath.add(it) }
        return classpath
    }
}