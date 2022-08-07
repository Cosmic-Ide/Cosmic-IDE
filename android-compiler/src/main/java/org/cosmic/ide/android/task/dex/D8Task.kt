package org.cosmic.ide.android.task.dex

import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode

import org.cosmic.ide.android.interfaces.*
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.project.JavaProject

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList

class D8Task : Task {

    @Throws(Exception::class)
    override fun doFullTask(project: JavaProject) {
            D8.run(
                    D8Command.builder()
                            .setOutput(Paths.get(project.getBinDirPath()), OutputMode.DexIndexed)
                            .addLibraryFiles(Paths.get(FileUtil.getClasspathDir(), "android.jar"))
                            .addProgramFiles(
                                    getClassFiles(File(project.getBinDirPath(), "classes")))
                            .build())
    }

    private fun getClassFiles(root: File) : ArrayList<Path> {
        val paths = arrayListOf<Path>()

        val files = root.listFiles()
        if (files == null) return paths
        for (f in files) {
            if (f.isFile()) {
                if (f.getName().endsWith(".class")) {
                    paths.add(f.toPath())
                }
            } else {
                paths.addAll(getClassFiles(f))
            }
        }
        return paths
    }

    override fun getTaskName() : String {
        return "D8 Task"
    }
}
