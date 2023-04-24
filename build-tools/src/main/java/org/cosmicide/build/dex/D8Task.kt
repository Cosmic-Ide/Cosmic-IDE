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
            toDex.forEach {
                reporter.reportInfo("Compiling library ${it.name}")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        compileJar(it, libDexDir.toPath())
                    } catch (e: Throwable) {
                        reporter.reportError("Error compiling library ${it.name}: ${e.stackTraceToString()}")
                    }
                }
            }
        }
    }

    /**
     * Compiles a list of jar files to a directory of dex files.
     *
     * @param jarFile The jar files to compile.
     * @param outputDir The directory to output the dex files to.
     */
    fun compileJar(jarFile: Path, outputDir: Path) {
        D8.run(
            D8Command.builder()
                .setMinApiLevel(MIN_API_LEVEL)
                .setMode(COMPILATION_MODE)
                .addClasspathFiles(getSystemClasspath().map { it.toPath() })
                .addProgramFiles(jarFile)
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
    fun getClassFilePaths(root: File): List<Path> {
        return root.walk().filter { it.extension == "class" }.map { it.toPath() }.toList()
    }

}