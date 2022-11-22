package org.cosmic.ide.android.task.kotlin

import android.content.SharedPreferences
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

    private val collector = object : MessageCollector {
        private val diagnostics = mutableListOf<Diagnostic>()

        override fun clear() { diagnostics.clear() }

        override fun hasErrors() = diagnostics.any { it.severity.isError }

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            // do not add redundant logging messages
            if (severity != CompilerMessageSeverity.LOGGING) {
                diagnostics += Diagnostic(severity, message, location)
            }
        }

        override fun toString() = diagnostics
            .joinToString(System.lineSeparator().repeat(2)) { it.toString() }
    }

    private val args = K2JVMCompilerArguments().apply {
        includeRuntime = false
        noReflect = true
        noStdlib = true
        noJdk = true
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
        val mKotlinHome = File(project.binDirPath, "kt_home").apply { mkdirs() }
        val mClassOutput = File(project.binDirPath, "classes").apply { mkdirs() }

        val claspath = arrayListOf<File>()
        val libs = File(project.libDirPath).listFiles()
        if (libs != null) {
            for (lib in libs) {
                claspath.add(lib)
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

        val cacheDir = File(project.binDirPath, "caches")

        makeIncrementally(
            cacheDir,
            listOf(File(project.srcDirPath)),
            args,
            collector
        )

        if (collector.hasErrors()) {
            throw CompilationFailedException(collector.toString())
        }
    }

    fun getSourceFiles(dir: File): ArrayList<String> {
        val sourceFiles = arrayListOf<String>()
        val files = dir.listFiles()
        if (files == null) return sourceFiles
        for (file in files) {
            if (file.isFile) {
                val extension = file.extension
                if (extension.equals("java") || extension.equals("kt")) {
                    sourceFiles.add(file.absolutePath)
                }
            } else {
                sourceFiles.addAll(getSourceFiles(file))
            }
        }
        return sourceFiles
    }

    private fun getKotlinCompilerPlugins(project: Project): List<File> {
        val pluginDir = File(project.projectDirPath, "kt_plugins")

        if (!pluginDir.exists() || pluginDir.isFile) {
            return listOf<File>()
        }

        val plugins = pluginDir.listFiles { file ->
            file.extension.equals("jar")
        }

        if (plugins == null) {
            return listOf<File>()
        }

        return plugins.toList()
    }

    override fun getTaskName(): String {
        return "Kotlin Compiler"
    }

    private data class Diagnostic(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?
    ) {
        override fun toString(): String {
            return severity.getPresentableName().uppercase() + ": " + location.toString().substringAfter("src/") + " " + message
        }
    }
}
