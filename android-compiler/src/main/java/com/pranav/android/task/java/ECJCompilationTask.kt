package com.pranav.android.task.java

import android.content.Context
import android.content.SharedPreferences

import com.pranav.android.exception.CompilationFailedException
import com.pranav.android.interfaces.*
import com.pranav.common.util.FileUtil
import com.pranav.project.mode.JavaProject

import org.eclipse.jdt.internal.compiler.batch.Main

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.PrintWriter
import java.util.ArrayList

class ECJCompilationTask(preferences: SharedPreferences) : Task {

    private val errs = StringBuilder()
    private val prefs: SharedPreferences

    init {
        prefs = preferences
    }

    override fun getTaskName(): String {
        return "ECJ Compilation Task"
    }

    @Throws(Exception::class)
    override fun doFullTask(project: JavaProject) {

        val writer =
                PrintWriter(
                        object : OutputStream() {
                            override fun write(p1: Int) {
                                errs.append(p1.toChar())
                            }
                        })

        val main = Main(writer, writer, false, null, null)

        val output = File(project.getBinDirPath(), "classes")

        val classpath = StringBuilder()
        classpath.append(FileUtil.getClasspathDir() + "android.jar")
        classpath.append(File.pathSeparator)
        classpath.append(FileUtil.getClasspathDir() + "core-lambda-stubs.jar")
        classpath.append(File.pathSeparator)
        classpath.append(FileUtil.getClasspathDir() + "kotlin-stdlib-1.7.10.jar")
        classpath.append(File.pathSeparator)
        classpath.append(output)
        val clspath = prefs.getString("classpath", "")
        if (!clspath!!.isEmpty() && classpath.length > 0) {
            classpath.append(File.pathSeparator)
            classpath.append(clspath)
        }
        var libs = new File(project.getLibDirPath()).listFiles();
        if (libs != null) {
            for (var lib : libs) {
                classpath.append(File.pathSeparator);
                classpath.append(lib.getAbsolutePath());
            }
        }

        val args = arrayListOf<String>(
            "-g",
            "-" + prefs.getString("version", "7"),
            "-d",
            output.getAbsolutePath(),
            "-cp",
            classpath.toString(),
            "-proc:none",
            "-sourcepath",
            " ",
            project.getSrcDirPath()
        )

        main.compile(args.toTypedArray())

        if (main.globalErrorsCount > 0 || !output.exists()) {
            throw CompilationFailedException(errs.toString())
        }
    }
}
