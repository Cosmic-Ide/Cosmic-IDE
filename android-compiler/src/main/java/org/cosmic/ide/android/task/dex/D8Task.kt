package org.cosmic.ide.android.task.dex

import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import org.cosmic.ide.CompilerUtil
import org.cosmic.ide.android.interfaces.Task
import org.cosmic.ide.project.Project
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class D8Task : Task {

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
                    .addClasspathFiles(CompilerUtil.platformPaths)
                    .addProgramFiles(Paths.get(jarFile))
                    .setOutput(Paths.get(dex).parent, OutputMode.DexIndexed)
                    .build()
            )
        }
    }

    /*
     * Compile class files of project to a Dalvik Executable (Dex) File.
     *
     * @param project the project to compile.
     */
    @Throws(Exception::class)
    override fun doFullTask(project: Project) {
        D8.run(
            D8Command.builder()
                .setMinApiLevel(26)
                .setMode(CompilationMode.DEBUG)
                .addClasspathFiles(CompilerUtil.platformPaths)
                .addProgramFiles(
                    getClassFiles(File(project.binDirPath, "classes"))
                )
                .setOutput(Paths.get(project.binDirPath), OutputMode.DexIndexed)
                .build()
        )
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
