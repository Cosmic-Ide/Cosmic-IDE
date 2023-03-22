package org.cosmicide.build.kotlin

import android.content.SharedPreferences
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

    private val args: K2JVMCompilerArguments by lazy {
        K2JVMCompilerArguments().apply {
            includeRuntime = false
            noReflect = true
            noStdlib = true
            noJdk = true
        }
    }

    @Throws(Exception::class)
    override fun execute(reporter: BuildReporter) {
        val sourceFiles = getSourceFiles(project.srcDir.invoke())
        if (!sourceFiles.any {
                it.extension == "kt"
            }
        ) {
            reporter.reportInfo("No Kotlin files are present. Skipping Kotlin compilation.")
            return
        }
        val mKotlinHome = File(project.binDir, "kotlin").apply { mkdirs() }
        val mClassOutput = File(project.binDir, "classes").apply { mkdirs() }

        val claspath = mutableListOf<File>()

        project.libDir.walk().forEach {
            if (it.extension == "jar") {
                claspath.add(it)
            }
        }

        val plugins = getKotlinCompilerPlugins(project).map(File::getAbsolutePath).toTypedArray()

        val appClass = Class.forName("org.cosmic.ide.App")
        val prefs =
            appClass.getDeclaredMethod("getDefaultPreferences").invoke(null) as SharedPreferences
        val useFastJarFS = prefs.getBoolean("fast_jar_fs", true)

        args.apply {
            classpath =
                getSystemClasspath().joinToString(separator = File.pathSeparator) { it.absolutePath } +
                        claspath.joinToString(
                            prefix = File.pathSeparator,
                            separator = File.pathSeparator
                        )
            kotlinHome = mKotlinHome.absolutePath
            destination = mClassOutput.absolutePath
            javaSourceRoots = sourceFiles.filter {
                it.extension == "java"
            }.map { it.absolutePath }.toTypedArray()
            // incremental compiler needs the module name for generating .kotlin_module files
            moduleName = project.name
            pluginClasspaths = plugins
            useFastJarFileSystem = useFastJarFS
        }

        val collector = object : MessageCollector {

            private var hasErrors: Boolean = false
            override fun clear() {}

            override fun hasErrors() = hasErrors

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                if (severity == CompilerMessageSeverity.ERROR) {
                    hasErrors = true
                }
                val diagnostic = Diagnostic(severity, message, location)
                when (severity) {
                    CompilerMessageSeverity.ERROR -> reporter.reportError(diagnostic.toString())
                    CompilerMessageSeverity.WARNING -> reporter.reportWarning(diagnostic.toString())
                    CompilerMessageSeverity.INFO -> reporter.reportInfo(diagnostic.toString())
                    CompilerMessageSeverity.LOGGING -> reporter.reportLogging(diagnostic.toString())
                    CompilerMessageSeverity.OUTPUT -> reporter.reportOutput(diagnostic.toString())
                    else -> reporter.reportInfo(diagnostic.toString())
                }
            }

            override fun toString() = ""
        }

        makeIncrementally(
            mKotlinHome,
            listOf(project.srcDir.invoke()),
            args,
            collector
        )
    }

    private fun getKotlinCompilerPlugins(project: Project): List<File> {
        val pluginDir = File(project.root, "kt_plugins")
        val plugins = mutableListOf<File>()

        pluginDir.walk().forEach {
            if (it.extension == "jar") {
                plugins.add(it)
            }
        }

        return plugins
    }

    private data class Diagnostic(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?
    ) {
        override fun toString(): String {
            return severity.presentableName.uppercase() + ": " + location.toString()
                .substringAfter("src/") + " " + message
        }
    }
}