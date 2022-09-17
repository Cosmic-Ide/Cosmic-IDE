package org.cosmic.ide.android.task.java

import android.content.SharedPreferences
import org.cosmic.ide.android.exception.CompilationFailedException
import org.cosmic.ide.android.interfaces.Task
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.project.Project
import org.eclipse.jdt.internal.compiler.batch.Main
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter

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
    override fun doFullTask(project: Project) {

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
        classpath.append(FileUtil.getClasspathDir() + "kotlin-stdlib-1.7.20-RC.jar")
        classpath.append(File.pathSeparator)
        classpath.append(FileUtil.getClasspathDir() + "kotlin-stdlib-common-1.7.20-RC.jar")
        classpath.append(File.pathSeparator)
        classpath.append(output)
        val clspath = prefs.getString("key_classpath", "")
        if (!clspath!!.isEmpty() && classpath.length > 0) {
            classpath.append(File.pathSeparator)
            classpath.append(clspath)
        }
        val libs = File(project.getLibDirPath()).listFiles()
        if (libs != null) {
            for (lib in libs) {
                classpath.append(File.pathSeparator)
                classpath.append(lib.absolutePath)
            }
        }

        val args = arrayListOf<String>(
            "-g",
            "-" + prefs.getString("key_java_version", "7"),
            "-d",
            output.getAbsolutePath(),
            "-cp",
            classpath.toString(),
            "-proc:none",
            "-sourcepath",
            " ", // This space is needed here
            project.getSrcDirPath()
        )

        main.compile(args.toTypedArray())

        if (main.globalErrorsCount > 0 || !output.exists()) {
            throw CompilationFailedException(errs.toString())
        }
    }
}
