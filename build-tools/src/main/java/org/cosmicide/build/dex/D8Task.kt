package org.cosmicide.build.dex

import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.DexIndexedConsumer.DirectoryConsumer
import com.android.tools.r8.OutputMode
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.build.util.getSystemClasspath
import org.cosmicide.project.Project
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class D8Task(
    val project: Project
) : Task {

    companion object {
        /*
        * Compile a jar file to a Dalvik Executable (Dex) File.
        *
        * @param jarFile the jar file to compile
        */
        @JvmStatic
        fun compileJar(jarFile: String) {
            val dex = jarFile.replaceAfterLast('.', "dex")
            D8.run(
                D8Command.builder()
                    .setMinApiLevel(26)
                    .setMode(CompilationMode.DEBUG)
                    .addClasspathFiles(getSystemClasspath().map { it.toPath() })
                    .addProgramFiles(Paths.get(jarFile))
                    .setOutput(Paths.get(dex).parent, OutputMode.DexIndexed)
                    .build()
            )
        }
    }

    /*
     * Compile a jar file to a Dalvik Executable (Dex) File.
     *
     * @param jarFile the jar file to compile
     */
    private fun compileJars(jarFiles: List<Path>, dir: Path) {
        D8.run(
            D8Command.builder()
                .setMinApiLevel(26)
                .setMode(CompilationMode.DEBUG)
                .addClasspathFiles(getSystemClasspath().map { it.toPath() })
                .addProgramFiles(jarFiles)
                .setOutput(dir, OutputMode.DexIndexed)
                .setProgramConsumer(
                    DirectoryConsumer(dir)
                )
                .build()
        )
    }

    /*
     * Compile class files of project to a Dalvik Executable (Dex) File.
     *
     * @param project the project to compile.
     */
    override fun execute(reporter: BuildReporter) {
        try {
            D8.run(
                D8Command.builder()
                    .setMinApiLevel(26)
                    .setMode(CompilationMode.DEBUG)
                    .addClasspathFiles(getSystemClasspath().map { it.toPath() })
                    .addProgramFiles(
                        getClassFiles(project.binDir.resolve("classes"))
                    )
                    .setOutput(project.binDir.toPath(), OutputMode.DexIndexed)
                    .build()
            )
        } catch (e: Exception) {
            reporter.reportError(e.stackTraceToString())
        }

        // Compile libraries
        val folder = project.libDir
        if (folder.exists() && folder.isDirectory) {
            val libs = folder.listFiles()
            if (libs != null) {
                val toDex = mutableListOf<Path>()
                val libDexes = File(project.buildDir, "libs")
                libDexes.mkdirs()
                // Check if all libs have been pre-dexed or not
                for (lib in libs) {
                    val outDex = File(libDexes, lib.nameWithoutExtension + ".dex")

                    if (lib.extension == "jar" && !outDex.exists()) {
                        toDex.add(lib.toPath())
                    }
                }
                try {
                    compileJars(toDex, project.buildDir.toPath().resolve("libs"))
                } catch (e: Exception) {
                    reporter.reportError(e.stackTraceToString())
                }
            }
        }
    }

    /*
     * Find all classes recursively in a directory.
     *
     * @param root directory to search in.
     */
    private fun getClassFiles(root: File): List<Path> {
        val paths = mutableListOf<Path>()

        root.walk().forEach {
            if (it.extension == "class") {
                paths.add(it.toPath())
            }
        }
        return paths
    }
}