/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

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
        compileKotlinCode()
        compileJavaCode()
        convertClassFilesToDexFormat()
        reporter.reportSuccess()
    }

    private inline fun <reified T : Task> compileTask(message: String) {
        val task = CompilerCache.getCache<T>()

        with(reporter) {
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
        object STARTED : BuildStatus()
        object FINISHED : BuildStatus()
    }
}