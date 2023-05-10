package org.cosmicide.rewrite.compile

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
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
class Compiler(
    private val project: Project,
    private val reporter: BuildReporter
) {
    companion object {
        var compileListener: (Class<*>, BuildStatus) -> Unit = { _, _ -> }

        fun initializeCache(project: Project) {
            CompilerCache.saveCache(JavaCompileTask(project))
            CompilerCache.saveCache(KotlinCompiler(project))
            CompilerCache.saveCache(D8Task(project))
        }
    }

    init {
        initializeCache(project)
    }

    /**
     * Compiles Kotlin and Java code and converts class files to dex format.
     */
    fun compile() {
        try {
            compileKotlinCode()
            compileJavaCode()
            convertClassFilesToDexFormat()
            reporter.reportSuccess()
        } catch (e: Exception) {
            reporter.reportError("Build failed: ${e.message}")
        }
    }

    private inline fun <reified T : Task> compileTask(message: String) {
        val task = CompilerCache.getCache<T>()

        with (reporter) {
            reportInfo(message)
            compileListener(T::class.java, BuildStatus.STARTED)
            task.execute(this)
            compileListener(T::class.java, BuildStatus.FINISHED)

            if (failure) {
                throw Exception("Failed to compile ${T::class.simpleName} code.")
            }

            reportInfo("Successfully compiled ${T::class.simpleName} code.")
        }
    }

    private fun compileJavaCode() {
        compileTask<JavaCompileTask>("Compiling Java code")
    }

    private fun compileKotlinCode() {
        compileTask<KotlinCompiler>("Compiling Kotlin code")
    }

    private fun convertClassFilesToDexFormat() {
        compileTask<D8Task>("Converting class files to dex format")
    }

    sealed class BuildStatus {
        object JAVA_COMPILING : BuildStatus()
        object KOTLIN_COMPILING : BuildStatus()
        object CONVERTING_TO_DEX : BuildStatus()
        object STARTED : BuildStatus()
        object FINISHED : BuildStatus()
    }
}