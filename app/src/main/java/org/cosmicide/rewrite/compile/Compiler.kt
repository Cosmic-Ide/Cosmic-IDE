package org.cosmicide.rewrite.compile

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.dex.D8Task
import org.cosmicide.build.java.JavaCompileTask
import org.cosmicide.build.kotlin.KotlinCompiler
import org.cosmicide.project.Project

/**
 * Provides a Compiler class which can compile Java and Kotlin code and convert class files to dex format.
 *
 * @param project The project to be compiled.
 * @param reporter The [BuildReporter] to report the build progress and status.
 */
class Compiler(val project: Project, val reporter: BuildReporter) {
    companion object {
        var compileListener: (Class<*>, BuildStatus) -> Unit = { _, _ -> {} }
    }

    init {
        CompilerCache.saveCache(JavaCompileTask(project))
        CompilerCache.saveCache(KotlinCompiler(project))
        CompilerCache.saveCache(D8Task(project))
    }

    /**
     * Compiles Java and Kotlin code and converts class files to dex format.
     */
    fun compile() {
        try {
            compileJava(reporter)
            compileKotlin(reporter)
            convertClassFilesToDex(reporter)
            reportSuccess(reporter)
        } catch (e: Exception) {
            reportFailure(reporter, e)
        }
    }

    private fun compileJava(reporter: BuildReporter) {
        reporter.reportInfo("Compiling Java code...")
        compileListener(JavaCompileTask::class.java, BuildStatus.STARTED)
        CompilerCache.getCache<JavaCompileTask>().execute(reporter)
        compileListener(JavaCompileTask::class.java, BuildStatus.FINISHED)
        if (reporter.failure) {
            throw Exception("Failed to compile Java code.")
        }
        reporter.reportInfo("Successfully compiled Java code.")
    }

    private fun compileKotlin(reporter: BuildReporter) {
        reporter.reportInfo("Compiling Kotlin code...")
        compileListener(KotlinCompiler::class.java, BuildStatus.STARTED)
        CompilerCache.getCache<KotlinCompiler>().execute(reporter)
        compileListener(KotlinCompiler::class.java, BuildStatus.FINISHED)
        if (reporter.failure) {
            throw Exception("Failed to compile Java code.")
        }
        reporter.reportInfo("Successfully compiled Kotlin code.")
    }

    private fun convertClassFilesToDex(reporter: BuildReporter) {
        reporter.reportInfo("Converting class files to dex format...")
        compileListener(D8Task::class.java, BuildStatus.STARTED)
        CompilerCache.getCache<D8Task>().execute(reporter)
        compileListener(D8Task::class.java, BuildStatus.FINISHED)
        if (reporter.failure) {
            throw Exception("Failed to compile Java code.")
        }
        reporter.reportInfo("Successfully converted class files to dex format.")
    }

    private fun reportSuccess(reporter: BuildReporter) {
        reporter.reportSuccess()
    }

    private fun reportFailure(reporter: BuildReporter, e: Exception) {
        reporter.reportError("Build failed: ${e.message}")
    }

    enum class BuildStatus {
        STARTED,
        FINISHED
    }
}