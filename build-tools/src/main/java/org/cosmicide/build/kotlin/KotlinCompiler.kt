package org.cosmicide.build.kotlin

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.build.util.getSourceFiles
import org.cosmicide.build.util.getSystemClasspath
import org.cosmicide.project.Project
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.makeIncrementally
import java.io.File

class KotlinCompiler(val project: Project) : Task {

    val args: K2JVMCompilerArguments by lazy {
        K2JVMCompilerArguments().apply {
            noReflect = true
            noStdlib = true
            noJdk = true
        }
    }

    override fun execute(reporter: BuildReporter) {
        val kotlinSourceFiles = getSourceFiles(project.srcDir.invoke(), "kt")
        if (kotlinSourceFiles.isEmpty()) {
            reporter.reportInfo("No Kotlin files are present. Skipping Kotlin compilation.")
            return
        }

        val kotlinHome = File(project.binDir, "kotlin").apply { mkdirs() }
        val classOutput = File(project.binDir, "classes").apply { mkdirs() }
        val classpath = collectClasspathFiles()

        val plugins = getKotlinCompilerPlugins().map(File::getAbsolutePath).toTypedArray()

        args.classpath = (getSystemClasspath() + classpath).joinToString(separator = File.pathSeparator) { it.absolutePath }
        args.kotlinHome = kotlinHome.absolutePath
        args.destination = classOutput.absolutePath
        args.javaSourceRoots = kotlinSourceFiles.filter { it.extension == "java" }.map { it.absolutePath }.toTypedArray()
        args.moduleName = project.name
        args.pluginClasspaths = plugins
        args.useFastJarFileSystem = true

        val collector = createMessageCollector(reporter)

        makeIncrementally(kotlinHome, listOf(project.srcDir.invoke()), args, collector)
    }

    fun collectClasspathFiles(): List<File> {
        val classpath = mutableListOf<File>()

        project.libDir.walk().forEach {
            if (it.isFile) {
                classpath.add(it)
            }
        }

        return classpath
    }

    fun getKotlinCompilerPlugins(): List<File> {
        val pluginDir = File(project.root, "kt_plugins")
        val plugins = mutableListOf<File>()

        pluginDir.walk().forEach {
            if (it.isFile) {
                plugins.add(it)
            }
        }

        return plugins
    }

    fun createMessageCollector(reporter: BuildReporter): MessageCollector {
        return object : MessageCollector {

            private var hasErrors: Boolean = false
            override fun clear() {}

            override fun hasErrors() = hasErrors

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                val diagnostic = Diagnostic(severity, message, location)
                when (severity) {
                    CompilerMessageSeverity.ERROR -> {
                        hasErrors = true
                        reporter.reportError(diagnostic.toString())
                    }
                    CompilerMessageSeverity.WARNING -> reporter.reportWarning(diagnostic.toString())
                    CompilerMessageSeverity.INFO -> reporter.reportInfo(diagnostic.toString())
                    CompilerMessageSeverity.LOGGING -> reporter.reportLogging(diagnostic.toString())
                    CompilerMessageSeverity.OUTPUT -> reporter.reportOutput(diagnostic.toString())
                    else -> reporter.reportInfo(diagnostic.toString())
                }
            }
        }
    }

    data class Diagnostic(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?
    ) {
        override fun toString() = "${severity.presentableName.uppercase()}: ${
            location?.toString()?.substringAfter("src/")
        } $message"
    }
}