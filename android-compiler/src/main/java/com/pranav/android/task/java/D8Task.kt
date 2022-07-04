package com.pranav.android.task.java

import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode

import com.pranav.android.interfaces.*
import com.pranav.common.util.FileUtil
import com.pranav.project.mode.JavaProject

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList

class D8Task() : Task {

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
        if (files != null) {
            for (f in files) {
                if (f.isFile()) {
                    paths.add(f.toPath())
                } else {
                    paths.addAll(getClassFiles(f))
                }
            }
        }
        return paths
    }

    override fun getTaskName() : String {
        return "D8 Task"
    }
}
