package com.pranav.lib_android.task.java

import com.pranav.lib_android.util.FileUtil
import com.pranav.lib_android.util.ConcurrentUtil
import com.pranav.lib_android.interfaces.Task
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import java.util.ArrayList
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class D8Task : Task() {

  @Throws(Throwable::class)
	override fun doFullTask() {
    ConcurrentUtil.execute {
      D8.run(
        D8Command.builder()
            .setOutput(Paths.get(FileUtil.getBinDir()), OutputMode.DexIndexed)
            .addLibraryFiles(Paths.get(FileUtil.getClasspathDir(), "android.jar"))
            .addProgramFiles(
                getClassFiles(
                    File(FileUtil.getBinDir(), "classes")
                )
            )
            .build()
      )
		}
	}
	
  private fun getClassFiles(root: File): List<Path> {
    val paths = ArrayList<Path>()
    val files = root.listFiles()
    if (files != null) {
      for (file in files) {
        if (file.isFile()) {
          paths.add(file.toPath())
        } else {
          paths.addAll(getClassFiles(file))
        }
      }
    }
    return paths.toList()
  }
	
	override fun getTaskName(): String {
		return "D8 Task"
	}
}
