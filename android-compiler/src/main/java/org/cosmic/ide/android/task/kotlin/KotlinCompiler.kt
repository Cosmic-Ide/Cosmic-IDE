package org.cosmic.ide.android.task.kotlin

import org.cosmic.ide.android.exception.CompilationFailedException
import org.cosmic.ide.android.interfaces.Task
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.project.Project
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunner
import org.jetbrains.kotlin.incremental.IncrementalFirJvmCompilerRunner
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotDisabled
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import java.io.File

/*
 * Copied from https://github.com/JetBrains/kotlin/blob/0b4a4ca42b96b9f4dfd32b4219f8afc034d9d766/compiler/incremental-compilation-impl/src/org/jetbrains/kotlin/incremental/IncrementalJvmCompilerRunner.kt#L71
 */
fun makeIncrementally(
    cachesDir: File,
    sourceRoots: Iterable<File>,
    args: K2JVMCompilerArguments,
    messageCollector: MessageCollector = MessageCollector.NONE
) {
    val kotlinExtensions = listOf("kt", "kts")
    val allExtensions = kotlinExtensions + "java"
    val rootsWalk = sourceRoots.asSequence().flatMap { it.walk() }
    val files = rootsWalk.filter(File::isFile)
    val sourceFiles = files.filter { it.extension.lowercase() in allExtensions }.toList()
    val buildHistoryFile = File(cachesDir, "build-history.bin")
    args.javaSourceRoots = sourceRoots.map { it.absolutePath }.toTypedArray()
    val buildReporter = BuildReporter(icReporter = DoNothingICReporter, buildMetricsReporter = DoNothingBuildMetricsReporter)

    withIC(args) {
        val compiler =
            if (args.useK2 && args.useFirIC && args.useFirLT)
                IncrementalFirJvmCompilerRunner(
                    cachesDir, buildReporter, buildHistoryFile, emptyList(), EmptyModulesApiHistory, kotlinExtensions, ClasspathSnapshotDisabled
                )
            else
                IncrementalJvmCompilerRunner(
                    cachesDir,
                    buildReporter,
                    // Use precise setting in case of non-Gradle build
                    usePreciseJavaTracking = !args.useK2,
                    outputFiles = emptyList(),
                    buildHistoryFile = buildHistoryFile,
                    modulesApiHistory = EmptyModulesApiHistory,
                    kotlinSourceFilesExtensions = kotlinExtensions,
                    classpathChanges = ClasspathSnapshotDisabled
                )
        compiler.compile(sourceFiles, args, messageCollector, providedChangedFiles = null)
    }
}

inline fun <R> withIC(args: CommonCompilerArguments, enabled: Boolean = true, fn: () -> R): R {
    val isEnabledBackup = IncrementalCompilation.isEnabledForJvm()
    IncrementalCompilation.setIsEnabledForJvm(enabled)

    try {
        if (args.incrementalCompilation == null) {
            args.incrementalCompilation = enabled
        }
        return fn()
    } finally {
        IncrementalCompilation.setIsEnabledForJvm(isEnabledBackup)
    }
}

object DoNothingICReporter : ICReporter {
    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {}
    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {}
    override fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String) {}
    override fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String) {}
    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {}
}

class KotlinCompiler : Task {

    @Throws(Exception::class)
    override fun doFullTask(project: Project) {
        val sourceFiles = getSourceFiles(File(project.getSrcDirPath()))
        if (!sourceFiles.any {
            it.endsWith(".kt")
        }
        ) {
            return
        }
        val mKotlinHome = File(project.getBinDirPath(), "kt_home").apply { mkdirs() }
        val mClassOutput = File(project.getBinDirPath(), "classes").apply { mkdirs() }

        val collector = object : MessageCollector {
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
        val claspath = arrayListOf<File>()
        val libs = File(project.getLibDirPath()).listFiles()
        if (libs != null) {
            for (lib in libs) {
                claspath.add(lib)
            }
        }

        val plugins = getKotlinCompilerPlugins(project).map(File::getAbsolutePath).toTypedArray()

        val args = K2JVMCompilerArguments().apply {
            useJavac = false
            compileJava = false
            includeRuntime = false
            noReflect = true
            noStdlib = true
            classpath =
                FileUtil.getClasspathDir() +
                "android.jar" +
                File.pathSeparator +
                FileUtil.getClasspathDir() +
                "core-lambda-stubs.jar" +
                File.pathSeparator +
                FileUtil.getClasspathDir() +
                "kotlin-stdlib-1.7.20-RC.jar" +
                File.pathSeparator +
                FileUtil.getClasspathDir() +
                "kotlin-stdlib-common-1.7.20-RC.jar" +
                claspath.joinToString(prefix = File.pathSeparator, separator = File.pathSeparator)
            kotlinHome = mKotlinHome.absolutePath
            destination = mClassOutput.absolutePath
            javaSourceRoots = sourceFiles.filter {
                it.endsWith(".java")
            }.toTypedArray()
            moduleName = project.getProjectName()
            pluginClasspaths = plugins
            noJdk = true
            useK2 = true
        }

        val cacheDir = File(project.getBinDirPath(), "caches")

        makeIncrementally(
            cacheDir,
            listOf(File(project.getSrcDirPath())),
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
            if (file.isFile()) {
                val path = file.extension
                if (path.equals("java") || path.equals("kt")) {
                    sourceFiles.add(file.absolutePath)
                }
            } else {
                sourceFiles.addAll(getSourceFiles(file))
            }
        }
        return sourceFiles
    }

    private fun getKotlinCompilerPlugins(project: Project): List<File> {
        val pluginDir = File(project.getProjectDirPath(), "kt_plugins")
        
        if (!pluginDir.exists() || pluginDir.isFile) {
            return listOf<File>()
        }
        
        val plugins = pluginDir.listFiles { file ->
            file.extension.equals("dex")
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
    )
}
