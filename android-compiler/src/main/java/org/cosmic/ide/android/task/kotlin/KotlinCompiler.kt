package org.cosmic.ide.android.task.kotlin

import android.content.SharedPreferences
import android.util.Log
import org.cosmic.ide.CompilerUtil
import org.cosmic.ide.android.exception.CompilationFailedException
import org.cosmic.ide.android.interfaces.Task
import org.cosmic.ide.project.Project
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.makeIncrementally
import java.io.File

class KotlinCompiler : Task {

    private val collector: MessageCollector by lazy {
        object : MessageCollector {
            private val diagnostics: MutableList<Diagnostic> by lazy {
                mutableListOf<Diagnostic>()
            }

            override fun clear() { diagnostics.clear() }

            override fun hasErrors() = diagnostics.any { it.severity.isError }

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                val diagnostic = Diagnostic(severity, message, location)
                // do not add redundant logging messages
                if (severity == CompilerMessageSeverity.LOGGING) {
                    Log.d("KotlinCompiler", diagnostic.toString())
                    return
                }
                diagnostics.add(diagnostic)
            }

            override fun toString() = diagnostics
                .joinToString(System.lineSeparator().repeat(2)) { it.toString() }
        }
    }

    private val args: K2JVMCompilerArguments by lazy {
        K2JVMCompilerArguments().apply {
            includeRuntime = false
            noReflect = true
            noStdlib = true
            noJdk = true
        }
    }

    @Throws(Exception::class)
    override fun doFullTask(project: Project) {
        val sourceFiles = getSourceFiles(File(project.srcDirPath))
        if (!sourceFiles.any {
            it.endsWith(".kt")
        }
        ) {
            return
        }
        val mKotlinHome = File(project.binDirPath, "kotlin").apply { mkdirs() }
        val mClassOutput = File(project.binDirPath, "classes").apply { mkdirs() }

        val claspath = mutableListOf<File>()

        File(project.libDirPath).walk().forEach {
            if (it.extension == "jar") {
                claspath.add(it)
            }
        }

        val plugins = getKotlinCompilerPlugins(project).map(File::getAbsolutePath).toTypedArray()

        val appClass = Class.forName("org.cosmic.ide.App")
        val prefs = appClass.getDeclaredMethod("getDefaultPreferences").invoke(null) as SharedPreferences
        val useFastJarFS = prefs.getBoolean("fast_jar_fs", true)

        args.apply {
            classpath =
                CompilerUtil.platformClasspath.joinToString(separator = File.pathSeparator) { it.absolutePath } +
                claspath.joinToString(prefix = File.pathSeparator, separator = File.pathSeparator)
            kotlinHome = mKotlinHome.absolutePath
            destination = mClassOutput.absolutePath
            javaSourceRoots = sourceFiles.filter {
                it.endsWith(".java")
            }.toTypedArray()
            // incremental compiler needs the module name for generating .kotlin_module files
            moduleName = project.projectName
            pluginClasspaths = plugins
            useFastJarFileSystem = useFastJarFS
        }

        collector.clear()

        makeIncrementally(
            mKotlinHome,
            listOf(File(project.srcDirPath)),
            args,
            collector
        )

        if (collector.hasErrors()) {
            throw CompilationFailedException(collector.toString())
        }
    }

    fun getSourceFiles(dir: File): List<String> {
        val sourceFiles = mutableListOf<String>()

        dir.walk().forEach {
            val ext = it.extension
            if (ext == "java" || ext == "kt") {
                sourceFiles.add(it.absolutePath)
            }
        }
        return sourceFiles
    }

    private fun getKotlinCompilerPlugins(project: Project): List<File> {
        val pluginDir = File(project.projectDirPath, "kt_plugins")
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
            return severity.presentableName.uppercase() + ": " + location.toString().substringAfter("src/") + " " + message
        }
    }
}
