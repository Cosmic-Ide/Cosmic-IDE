/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.compile

import org.cosmicide.App
import org.cosmicide.R
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.build.dex.D8Task
import org.cosmicide.build.java.JarTask
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
        /**
         * A listener to be called when a compiler starts or finishes compiling.
         */
        @JvmStatic
        var compileListener: (Class<*>, BuildStatus) -> Unit = { _, _ -> }

        /**
         * Initializes the cache of compiler instances.
         */
        @JvmStatic
        fun initializeCache(project: Project) {
            CompilerCache.saveCache(JavaCompileTask(project))
            CompilerCache.saveCache(KotlinCompiler(project))
            CompilerCache.saveCache(D8Task(project))
            CompilerCache.saveCache(JarTask(project))
        }
    }

    private val context = App.instance.get()!!

    init {
        initializeCache(project)
    }

    /**
     * Compiles Kotlin and Java code and converts class files to dex format.
     */
    fun compile(release: Boolean = false) {
        compileKotlinCode()
        compileJavaCode()
        convertClassFilesToDexFormat()
        if (release) {
            compileJar()
        }
        reporter.reportSuccess()
    }

    /**
     * Compiles a task.
     *
     * @param T The type of the task to be compiled.
     * @param message The message to be reported when the task starts compiling.
     */
    private inline fun <reified T : Task> compileTask(message: String) {
        val task = CompilerCache.getCache<T>()

        with(reporter) {
            if (failure) return
            reportInfo(message)
            compileListener(T::class.java, BuildStatus.STARTED)
            task.execute(this)
            compileListener(T::class.java, BuildStatus.FINISHED)

            if (failure) {
                reportOutput(context.getString(R.string.failed_to_compile, T::class.simpleName))
            }

            reportInfo(context.getString(R.string.successfully_run, T::class.simpleName))
        }
    }

    /**
     * Compiles Java code.
     */
    private fun compileJavaCode() {
        compileTask<JavaCompileTask>(context.getString(R.string.compiling_java))
    }

    /**
     * Compiles the classes directory to `classes.jar`.
     */
    private fun compileJar() {
        compileTask<JarTask>(context.getString(R.string.assembling_jar))
    }

    /**
     * Compiles Kotlin code.
     */
    private fun compileKotlinCode() {
        compileTask<KotlinCompiler>(context.getString(R.string.compiling_kotlin))
    }

    /**
     * Converts class files to dex format.
     */
    private fun convertClassFilesToDexFormat() {
        compileTask<D8Task>(context.getString(R.string.compiling_class_files_to_dex))
    }

    sealed class BuildStatus {
        data object STARTED : BuildStatus()
        data object FINISHED : BuildStatus()
    }
}