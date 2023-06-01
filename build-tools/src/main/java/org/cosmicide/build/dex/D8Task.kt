/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.build.dex

import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.build.util.getSystemClasspath
import org.cosmicide.project.Project
import org.cosmicide.rewrite.common.Prefs
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Task to compile the class files of a project to a Dalvik Executable (Dex) file using D8.
 *
 * @property project The project to compile.
 */
class D8Task(val project: Project) : Task {

    companion object {
        const val MIN_API_LEVEL = 26
        val COMPILATION_MODE = CompilationMode.DEBUG
    }

    /**
     * Compiles the project classes to a Dex file.
     *
     * @param reporter The BuildReporter instance to report any errors to.
     */
    override fun execute(reporter: BuildReporter) {
        if (Prefs.useSSVM) {
            reporter.reportInfo("Skipping D8 compilation because SSVM is enabled.")
            return
        }
        val classes = getClassFiles(project.binDir.resolve("classes"))
        if (classes.isEmpty()) {
            reporter.reportError("No classes found to compile.")
            return
        }
        D8.run(
            D8Command.builder()
                .setMinApiLevel(MIN_API_LEVEL)
                .setMode(COMPILATION_MODE)
                .addClasspathFiles(getSystemClasspath().map { it.toPath() })
                .addProgramFiles(classes)
                .setOutput(project.binDir.toPath(), OutputMode.DexIndexed)
                .build()
        )

        // Compile libraries
        val libDir = project.libDir
        if (libDir.exists() && libDir.isDirectory) {
            val libDexDir = project.buildDir.resolve("libs").apply { mkdirs() }
            libDir.listFiles { file -> file.extension == "jar" }?.mapNotNull { lib ->
                val outDex = libDexDir.resolve(lib.nameWithoutExtension + ".dex")
                if (!outDex.exists()) lib.toPath() else null
            }?.forEach { jarFile ->
                reporter.reportInfo("Compiling library ${jarFile.name}")
                CoroutineScope(Dispatchers.IO).launch {
                    compileJar(jarFile, libDexDir.toPath(), reporter)
                }
            }
        }
    }

    /**
     * Compiles a jar file to a directory of dex files.
     *
     * @param jarFile The jar file to compile.
     * @param outputDir The directory to output the dex files to.
     * @param reporter The BuildReporter instance to report any errors to.
     */
    fun compileJar(jarFile: Path, outputDir: Path, reporter: BuildReporter) {
        try {
            D8.run(
                D8Command.builder()
                    .setMinApiLevel(MIN_API_LEVEL)
                    .setMode(COMPILATION_MODE)
                    .addClasspathFiles(getSystemClasspath().map { it.toPath() })
                    .addProgramFiles(jarFile)
                    .setOutput(outputDir, OutputMode.DexIndexed)
                    .build()
            )
        } catch (e: Throwable) {
            reporter.reportError(e.stackTraceToString())
        }
    }

    /**
     * Returns a list of paths to all class files recursively in a directory.
     *
     * @param root The directory to search in.
     * @return A list of paths to all class files in the directory.
     */
    fun getClassFiles(root: File): List<Path> {
        return root.walk().filter { it.extension == "class" }.map { it.toPath() }.toList()
    }
}