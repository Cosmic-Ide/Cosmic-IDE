package com.pranav.android.task.java

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File

import com.pranav.common.util.FileUtil
import com.pranav.android.interfaces.*
import com.pranav.project.mode.JavaProject
import com.pranav.android.exception.CompilationFailedException

class KotlinCompiler() : Task {

    @Throws(IOException::class)
    override fun doFullTask(project: JavaProject) {
        val mKotlinHome  = File(project.getBinDirPath(), "kt_home").apply { mkdirs() }
        val mClassOutput = File(project.getBinDirPath(), "classes").apply { mkdirs() }

        val compiler = K2JVMCompiler()
        val collector = object : MessageCollector {
            private val diagnostics = mutableListOf<Diagnostic>()

            override fun clear() { diagnostics.clear() }

            override fun hasErrors() = diagnostics.any { it.severity.isError }

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                diagnostics += Diagnostic(severity, message, location)
            }

            override fun toString() = diagnostics
                .joinToString(System.lineSeparator().repeat(2)) { it.toString() }
        }

        val arguments = mutableListOf<String>().apply {
            // Classpath
            add("-cp")
            add(FileUtil.getClasspathDir() + "android.jar")
            add(File.pathSeparator)
            add(FileUtil.getClasspathDir() + "core-lambda-stubs.jar")

            // Sources (.java & .kt)
            add(project.getSrcDirPath())
        }

        val args = K2JVMCompilerArguments().apply {
            compileJava = false
            includeRuntime = false
            noJdk = true
            noReflect = true
            noStdlib = true
            kotlinHome = mKotlinHome.absolutePath
            destination = mClassOutput.absolutePath
        }

        Log.d("TAG", "Running kotlinc with these arguments: $arguments")

        compiler.parseArguments(arguments.toTypedArray(), args)
        compiler.exec(collector, Services.EMPTY, args)
        throw CompilationFailedException(collector.toString())
    }

    fun getSourceFiles(path: File): ArrayList<File> {
        val sourceFiles = arrayListOf<File>()
        val files = path.listFiles()
        if (files == null) {
            return arrayListOf<File>()
        }
        for (file in files) {
            if (file.isFile()) {
                if (file.getName().endsWith(".java") || file.getName().endsWith(".kt")) {
                    sourceFiles.add(file)
                }
            } else {
                sourceFiles.addAll(getSourceFiles(file))
            }
        }
        return sourceFiles
    }
    

    private data class Diagnostic(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?
    )

}
