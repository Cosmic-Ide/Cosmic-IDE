package org.cosmicide.build.dex

import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.build.util.getSystemClasspath
import org.cosmicide.project.Project
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Task to compile the class files of a project to a Dalvik Executable (Dex) file using D8.
 *
 * @property project The project to compile.
 */
class D8Task(private val project: Project) : Task {

    companion object {
        private const val MIN_API_LEVEL = 26
        private val COMPILATION_MODE = CompilationMode.DEBUG
    }

    /**
     * Compiles the project classes to a Dex file.
     *
     * @param reporter The BuildReporter instance to report any errors to.
     */
    override fun execute(reporter: BuildReporter) {
        try {
            D8.run(
                D8Command.builder()
                    .setMinApiLevel(MIN_API_LEVEL)
                    .setMode(COMPILATION_MODE)
                    .addClasspathFiles(getSystemClasspath().map { it.toPath() })
                    .addProgramFiles(
                        getClassFilePaths(project.binDir.resolve("classes"))
                    )
                    .setOutput(project.binDir.toPath(), OutputMode.DexIndexed)
                    .build()
            )
        } catch (e: Exception) {
            reporter.reportError("Error compiling project classes: ${e.message}")
        }

        // Compile libraries
        val libDir = project.libDir
        if (libDir.exists() && libDir.isDirectory) {
            val toDex = mutableListOf<Path>()
            val libDexDir = File(project.buildDir, "libs").apply { mkdirs() }
            // Check if all libs have been pre-dexed or not
            libDir.listFiles()?.forEach { lib ->
                val outDex = File(libDexDir, lib.nameWithoutExtension + ".dex")
                if (lib.extension == "jar" && !outDex.exists()) {
                    toDex.add(lib.toPath())
                }
            }
            if (toDex.isNotEmpty()) {
                compileJars(toDex, libDexDir.toPath())
            }
        }
    }

    /**
     * Compiles a list of jar files to a directory of dex files.
     *
     * @param jarFiles The jar files to compile.
     * @param outputDir The directory to output the dex files to.
     */
    private fun compileJars(jarFiles: List<Path>, outputDir: Path) {
        D8.run(
            D8Command.builder()
                .setMinApiLevel(MIN_API_LEVEL)
                .setMode(COMPILATION_MODE)
                .addClasspathFiles(getSystemClasspath().map { it.toPath() })
                .addProgramFiles(jarFiles)
                .setOutput(outputDir, OutputMode.DexIndexed)
                .build()
        )
    }

    /**
     * Returns a list of paths to all class files recursively in a directory.
     *
     * @param root The directory to search in.
     * @return A list of paths to all class files in the directory.
     */
    private fun getClassFilePaths(root: File): List<Path> {
        return root.walk().filter { it.extension == "class" }.map { it.toPath() }.toList()
    }

}