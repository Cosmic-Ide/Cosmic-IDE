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

class D8Task: Task() {


	override fun doFullTask() {
    var ex: Exception = null
		ConcurrentUtil.execute({
			try {
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

			} catch (e: Exception) {
				ex = e
			}
		})
		if (ex != null) {
			throw ex!!
		}
	}
	
  private fun getClassFiles(root: File): ArrayList<Path> {
    val paths = ArrayList<Path>()
    val walker = root.walk()
    walker.iterator().forEach {
      if (it.isFile()) {
        paths.add(it.toPath())
      } else {
        paths.addAll(getClassFiles(it))
      }
    }
    return paths
  }
	
	override fun getTaskName(): String {
		return "D8 Task"
	}
}
