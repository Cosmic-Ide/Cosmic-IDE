package org.cosmicide.build.kotlin

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.build.util.getSourceFiles
import org.cosmicide.build.util.getSystemClasspath
import org.cosmicide.project.Project
import org.cosmicide.rewrite.common.Prefs
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.makeIncrementally
import java.io.File

class KotlinCompiler(val project: Project) : Task {

    val args: K2JVMCompilerArguments by lazy {
        K2JVMCompilerArguments().apply {
            noReflect = true
            noStdlib = true
            noJdk = true
            newInference = true
        }
    }

    override fun execute(reporter: BuildReporter) {
        val sourceFiles = getSourceFiles(project.srcDir, "kt")
        if (sourceFiles.isEmpty()) {
            reporter.reportInfo("No Kotlin files are present. Skipping Kotlin compilation.")
            return
        }

        val kotlinHomeDir = File(project.binDir, "kotlin").apply { mkdirs() }
        val classOutput = File(project.binDir, "classes").apply { mkdirs() }
        val classpathFiles = collectClasspathFiles()

        val enabledPlugins = getKotlinCompilerPlugins().map(File::getAbsolutePath).toTypedArray()

        args.apply {
            classpath =
                (getSystemClasspath() + classpathFiles).joinToString(separator = File.pathSeparator) { it.absolutePath }
            kotlinHome = kotlinHomeDir.absolutePath
            destination = classOutput.absolutePath
            javaSourceRoots =
                sourceFiles.filter { it.isJavaFile() }.map { it.absolutePath }.toTypedArray()
            moduleName = project.name
            pluginClasspaths = enabledPlugins
            useFastJarFileSystem = Prefs.useFastJarFs
            useFirIC = true
            useFirLT = true
            if (Prefs.useK2) languageVersion = "2.0"
        }

        val collector = createMessageCollector(reporter)

        makeIncrementally(kotlinHomeDir, listOf(project.srcDir), args, collector)
    }

    fun collectClasspathFiles(): List<File> {
        return project.libDir.walk().filter(File::isFile).toList()
    }

    fun getKotlinCompilerPlugins(): List<File> {
        val pluginDir = File(project.root, "kt_plugins")

        return pluginDir.walk().filter(File::isFile).toList()
    }

    fun createMessageCollector(reporter: BuildReporter): MessageCollector =
        object : MessageCollector {

            private var hasErrors: Boolean = false

            override fun clear() {}

            override fun hasErrors() = hasErrors

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                val diagnostic = CompilationDiagnostic(message, location)
                when (severity) {
                    CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> {
                        hasErrors = true
                        reporter.reportError(diagnostic.toString())
                    }
                    CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> reporter.reportWarning(diagnostic.toString())
                    CompilerMessageSeverity.INFO -> reporter.reportInfo(diagnostic.toString())
                    CompilerMessageSeverity.LOGGING -> reporter.reportLogging(diagnostic.toString())
                    CompilerMessageSeverity.OUTPUT -> reporter.reportOutput(diagnostic.toString())
                }
            }
        }

    data class CompilationDiagnostic(
        val message: String,
        val location: CompilerMessageSourceLocation?
    ) {
        override fun toString() =
            location?.toString()?.substringAfter("src/main/").orEmpty() + " " + message
    }
}