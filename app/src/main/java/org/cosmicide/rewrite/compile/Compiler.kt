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
 */
class Compiler(private val project: Project) {

    /**
     * Lazily initializes a [CompilerCache] instance to store compiled code.
     */
    private val compilerCache: CompilerCache by lazy {
        CompilerCache(
            JavaCompileTask(project),
            KotlinCompiler(project),
            D8Task(project)
        )
    }

    /**
     * Compiles Java and Kotlin code and converts class files to dex format.
     *
     * @param reporter The [BuildReporter] to report the build progress and status.
     */
    fun compile(reporter: BuildReporter) {
        try {
            compileJava(reporter)
            compileKotlin(reporter)
            convertClassFilesToDex(reporter)
            reportSuccess(reporter)
        } catch (e: Exception) {
            reportFailure(reporter, e)
        }
    }

    /**
     * Compiles the Java code using [JavaCompileTask] and reports the progress and status.
     *
     * @param reporter The [BuildReporter] to report the build progress and status.
     */
    private fun compileJava(reporter: BuildReporter) {
        reporter.reportInfo("Compiling Java code...")
        compilerCache.javaCompiler.execute(reporter)
        reporter.reportInfo("Successfully compiled Java code.")
    }

    /**
     * Compiles the Kotlin code using [KotlinCompiler] and reports the progress and status.
     *
     * @param reporter The [BuildReporter] to report the build progress and status.
     */
    private fun compileKotlin(reporter: BuildReporter) {
        reporter.reportInfo("Compiling Kotlin code...")
        compilerCache.kotlinCompiler.execute(reporter)
        reporter.reportInfo("Successfully compiled Kotlin code.")
    }

    /**
     * Converts the class files to dex format using [D8Task] and reports the progress and status.
     *
     * @param reporter The [BuildReporter] to report the build progress and status.
     */
    private fun convertClassFilesToDex(reporter: BuildReporter) {
        reporter.reportInfo("Converting class files to dex format...")
        compilerCache.dexTask.execute(reporter)
        reporter.reportInfo("Successfully converted class files to dex format.")
    }

    /**
     * Reports the build success using the provided [BuildReporter].
     *
     * @param reporter The [BuildReporter] to report the build progress and status.
     */
    private fun reportSuccess(reporter: BuildReporter) {
        reporter.reportSuccess()
    }

    /**
     * Reports the build failure using the provided [BuildReporter] and the given [e] exception.
     *
     * @param reporter The [BuildReporter] to report the build progress and status.
     * @param e The [Exception] that caused the build failure.
     */
    private fun reportFailure(reporter: BuildReporter, e: Exception) {
        reporter.reportError("Build failed: ${e.message}")
    }
}