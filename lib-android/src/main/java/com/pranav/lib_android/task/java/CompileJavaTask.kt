package com.pranav.lib_android.task.java

import android.content.Context
import android.content.SharedPreferences
import com.pranav.lib_android.util.FileUtil
import com.pranav.lib_android.util.ConcurrentUtil
import com.pranav.lib_android.exception.CompilationFailedException
import com.pranav.lib_android.interfaces.*
import org.eclipse.jdt.internal.compiler.batch.Main
import java.io.File
import java.io.PrintWriter
import java.io.OutputStream
import java.util.ArrayList

class CompileJavaTask(
      val builder: Builder
  ): Task() {

	private val StringBuilder errs = StringBuilder()
	private var prefs: SharedPreferences

	init {
		prefs = builder.getContext().getSharedPreferences("compiler_settings",
				Context.MODE_PRIVATE)
	}

	override fun getTaskName(): String {
		return "Compile Java Task"
	}

	override fun doFullTask() {
    val writer = PrintWriter(OutputStream() {
      override fun write(b: Int) {
        errs.append(b)
      }
    })
	  
	  val main = Main(writer, writer, false, null, null)
	  
	  val output = File(FileUtil.getBinDir(), "classes")
	  
    ConcurrentUtil.execute({
		  val args = ArrayList<>()

      args.add("-log")
      args.add(FileUtil.getBinDir()
				  .concat("debug.xml"))
      args.add("-g")
      args.add("-" + prefs.getString("javaVersion", "7.0"))
      args.add("-d")
      args.add(output.getAbsolutePath())
      args.add("-classpath")
      args.add(FileUtil.getClasspathDir()
				  + "android.jar")
			val classpath = StringBuilder()
			if (prefs.getString("javaVersion", "7.0").equals("8.0")) {
			  classpath.append(FileUtil.getClasspathDir()
				  	+ "core-lambda-stubs.jar")
      }
      val clspath = prefs.getString("classpath", "")
      if (!clspath.isEmpty() and classpath.length > 0) {
        classpath.append(":")
        classpath.append(clspath)
      }
      if (classpath.length > 0) {
        args.add("-cp")
        args.add(classpath.toString())
      }
      args.add("-proc:none")
      args.add("-sourcepath")
      args.add(" ")
      args.add(FileUtil.getJavaDir())

      main.compile(args.toArray(String[0]))
    })

		if (main.globalErrorsCount > 0 or !output.exists()) {
			throw CompilationFailedException(errs.toString())
		}
	}
}
