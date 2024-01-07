/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.build.dex

import android.os.Build
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.build.util.getSystemClasspath
import org.cosmicide.project.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.nameWithoutExtension

/**
 * Task to compile the class files of a project to a Dalvik Executable (Dex) file using D8.
 *
 * @property project The project to compile.
 */
class D8Task(val project: Project) : Task {

    companion object {
        const val MIN_API_LEVEL = Build.VERSION_CODES.O

        val COMPILATION_MODE = CompilationMode.DEBUG

        /**
         * Compiles a jar file to a directory of dex files.
         *
         * @param jarFile The jar file to compile.
         * @param outputDir The directory to output the dex files to.
         * @param reporter The BuildReporter instance to report any errors to.
         */
        fun compileJar(jarFile: Path, outputDir: Path, reporter: BuildReporter? = null) {
            val zipFile = ZipFile(jarFile.toFile())

            // If the jar has no files with the .class extension, skip it
            if (zipFile.use { zip ->
                    zip.entries().asSequence()
                        .none { it.name.startsWith("META-INF").not() && it.name.endsWith(".class") }
                }) {
                return
            }

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
                Files.move(
                    outputDir.resolve("classes.dex"),
                    outputDir.resolve(jarFile.nameWithoutExtension + ".dex")
                )
            } catch (e: Throwable) {
                reporter?.reportError(e.stackTraceToString())
            }
        }
    }

    /**
     * Compiles the project classes to a Dex file.
     *
     * @param reporter The BuildReporter instance to report any errors to.
     */
    override fun execute(reporter: BuildReporter) {
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
            libDir.listFiles { file -> file.extension == "jar" }?.filter { lib ->
                libDexDir.resolve(lib.nameWithoutExtension + ".dex").exists().not()
            }?.forEach { jarFile ->
                reporter.reportInfo("Compiling library ${jarFile.name}")
                compileJar(jarFile.toPath(), libDexDir.toPath(), reporter)
            }
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
