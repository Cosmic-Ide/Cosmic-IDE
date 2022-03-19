package com.pranav.lib_android.task.java

import android.content.Context
import android.content.SharedPreferences
import com.pranav.lib_android.FileUtil
import com.pranav.lib_android.exception.CompilationFailedException
import com.pranav.lib_android.interfaces.*
import org.eclipse.jdt.internal.compiler.batch.Main
import java.io.*
import java.util.ArrayList

class CompileJavaTask: Task() {

	var mBuilder: Builder
	val errs = StringBuilder()
	var prefs: SharedPreferences

	constructor(builder: Builder) {
		this.mBuilder = builder
		prefs = mBuilder.getContext().getSharedPreferences("compiler_settings",
				Context.MODE_PRIVATE)
	}

	override fun getTaskName(): String {
		return this.getClass().getSimpleName()
	}

	override fun doFullTask() {

        val output = File(FileUtil.getBinDir() + "classes")
		val args = ArrayList<String>()

		args.add("-warn:all")
		args.add("-log")
		args.add(FileUtil.getBinDir()
				.concat("debug.xml"))
		args.add("-g")
		args.add("-" + prefs.getString("javaVersion", "1.7"))
		args.add("-d")
		args.add(output.getAbsolutePath())
		args.add("-bootclasspath")
		args.add(FileUtil.getClasspathDir()
				+ "android.jar")
		val classpath = StringBuilder()
		if (prefs.getString("javaVersion", "1.7").equals("1.8")) {
			classpath.append(FileUtil.getClasspathDir()
					+ "core-lambda-stubs.jar")
		}
		val clspath = prefs.getString("classpath", "")
		if (clspath != "") {
			if (classpath.toString() != "") classpath.append(":")
			classpath.append(clspath)
		}
		if (classpath.toString() != "") {
			args.add("-cp")
			args.add(classpath.toString())
		}
		args.add("-proc:none")
		args.add("-sourcepath")
		args.add(" ")
		args.add(FileUtil.getJavaDir())

		val writer = PrintWriter((p1) -> errs.append((char) p1))

		val main = Main(writer, writer, false, null, null)

		main.compile(args.toArray(String[0]))

		if (main.globalErrorsCount > 0 | !output.exists()) {
			throw CompilationFailedException(errs.toString())
		}
	}
}