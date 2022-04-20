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

	val errs = StringBuilder()
	var prefs: SharedPreferences

	init {
		prefs = builder.getContext().getSharedPreferences("compiler_settings",
				Context.MODE_PRIVATE)
	}

	override fun getTaskName(): String {
		return "Compile Java Task"
	}

  @Throws(CompilationFailedException::class)
	override fun doFullTask() {
    val writer = PrintWriter(
        object : OutputStream() {
          override fun write(p1: Int) {
            errs.append(p1.toChar())
          }
        }
    )
	  
	  val main = Main(writer, writer, false, null, null)
	  
	  val output = File(FileUtil.getBinDir(), "classes")
	  
    ConcurrentUtil.execute {
      val version = prefs.getFloat("javaVersion", 7.0f)
      val args = ArrayList<String>()

      args.add("-log")
      args.add(FileUtil.getBinDir()
				  .plus("debug.xml"))
      args.add("-g")
      args.add("-" + version.toString())
      args.add("-d")
      args.add(output.getAbsolutePath())
      args.add("-classpath")
      args.add(FileUtil.getClasspathDir()
				  .plus("android.jar"))
			val classpath = StringBuilder()
			if (version >= 8.0) {
			  classpath.append(FileUtil.getClasspathDir()
				  	+ "core-lambda-stubs.jar")
      }
      val clspath: String? = prefs.getString("classpath", "")
      if (clspath!!.length > 0 && classpath.length > 0) {
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

      main.compile(args.toTypedArray())
    }

		if (main.globalErrorsCount > 0 || !output.exists()) {
			throw CompilationFailedException(errs.toString())
		}
	}
}
