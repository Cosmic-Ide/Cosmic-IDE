package org.cosmicide.rewrite.compile

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.dex.D8Task
import org.cosmicide.build.java.JavaCompileTask
import org.cosmicide.build.kotlin.KotlinCompiler
import org.cosmicide.project.Project

/**
 * A class responsible for compiling Java and Kotlin code and converting class files to dex format.
 *
 * @property project The project to be compiled.
 * @property reporter The [BuildReporter] to report the build progress and status.
 */
class Compiler(val project: Project, val reporter: BuildReporter) {
    companion object {
        var compileListener: (Class<*>, BuildStatus) -> Unit = { _, _ -> }
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
            compileKotlin()
            compileJava()
            convertClassFilesToDex()
            reportSuccess()
        } catch (e: Exception) {
            reportFailure(e)
        }
    }

    private fun compileJava() {
        reporter.reportInfo("Compiling Java code...")
        compileListener(JavaCompileTask::class.java, BuildStatus.STARTED)
        CompilerCache.getCache<JavaCompileTask>().execute(reporter)
        compileListener(JavaCompileTask::class.java, BuildStatus.FINISHED)

        if (reporter.failure) {
            throw Exception("Failed to compile Java code.")
        }

        reporter.reportInfo("Successfully compiled Java code.")
    }

    private fun compileKotlin() {
        reporter.reportInfo("Compiling Kotlin code...")
        compileListener(KotlinCompiler::class.java, BuildStatus.STARTED)
        CompilerCache.getCache<KotlinCompiler>().execute(reporter)
        compileListener(KotlinCompiler::class.java, BuildStatus.FINISHED)

        if (reporter.failure) {
            throw Exception("Failed to compile Kotlin code.")
        }

        reporter.reportInfo("Successfully compiled Kotlin code.")
    }

    private fun convertClassFilesToDex() {
        reporter.reportInfo("Converting class files to dex format...")
        compileListener(D8Task::class.java, BuildStatus.STARTED)
        CompilerCache.getCache<D8Task>().execute(reporter)
        compileListener(D8Task::class.java, BuildStatus.FINISHED)

        if (reporter.failure) {
            throw Exception("Failed to compile dex files.")
        }

        reporter.reportInfo("Successfully converted class files to dex format.")
    }

    private fun reportSuccess() {
        reporter.reportSuccess()
    }

    private fun reportFailure(e: Exception) {
        reporter.reportError("Build failed: ${e.message}")
    }

    enum class BuildStatus {
        STARTED,
        FINISHED
    }
}