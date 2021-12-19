package com.pranav.java.lib_android.java

import com.pranav.java.lib_android.CompilationFailedException
import com.pranav.java.lib_android.Task
import org.eclipse.jdt.internal.compiler.batch.Main
import java.io.OutputStream
import java.io.PrintWriter

class CompileJavaTask : Task() {
    override fun doFullTask() {
        val args = ArrayList<String>()
        args.add("-7")
        args.add("-nowarn")
        args.add("-deprecation")
        args.add("-d")
        args.add("/storage/emulated/0/Android/data/com.pranav.ide/files/test/android/classes/")
        args.add("-cp")
        args.add("/storage/emulated/0/Android/data/com.pranav.ide/files/classpath/android.jar")
        args.add("-proc:none")
        args.add("-sourcepath")
        args.add("/storage/emulated/0/Android/data/com.pranav.ide/files/java/Main.java")
        val err = StringBuilder()
        val writer = PrintWriter(
            object : OutputStream() {
                override fun write(p1: Int) {
                    err.append(p1.toChar())
                }
            })
        val main = Main(writer, writer, false, null, null)
        main.compile(args.toTypedArray())
        if (main.globalErrorsCount > 0) {
            throw CompilationFailedException(err.toString())
        }
    }

    override fun getTaskName(): String {
        return "Compile java task"
    }
}