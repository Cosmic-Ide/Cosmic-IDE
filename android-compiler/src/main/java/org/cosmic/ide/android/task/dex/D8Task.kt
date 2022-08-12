package org.cosmic.ide.android.task.dex

import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode

import org.cosmic.ide.android.interfaces.Task
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.project.JavaProject

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class D8Task : Task {

    @Throws(Exception::class)
    override fun doFullTask(project: JavaProject) {
            D8.run(
                    D8Command.builder()
                            .setMinApiLevel(26)
                            .addLibraryFiles(Paths.get(FileUtil.getClasspathDir(), "android.jar"))
                            .addProgramFiles(
                                getClassFiles(File(project.getBinDirPath(), "classes")))
                            .setOutput(Paths.get(project.getBinDirPath()), OutputMode.DexIndexed)
                            .build())
    }

    private fun getClassFiles(root: File) : ArrayList<Path> {
        val paths = arrayListOf<Path>()

        root.walk().forEach {
            if (it.isFile() && it.extension == "class") {
                paths.add(it.toPath())
            }
        }
        return paths
    }

    override fun getTaskName() : String {
        return "D8 Task"
    }
}
