package com.pranav.android.task.java

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunner
import java.io.File

import com.pranav.common.util.FileUtil
import com.pranav.android.interfaces.*
import com.pranav.project.mode.JavaProject
import com.pranav.android.exception.CompilationFailedException

class KotlinCompiler() : Task {

    @Throws(Exception::class)
    override fun doFullTask(project: JavaProject) {
        val sourceFiles = getSourceFiles(File(project.getSrcDirPath()))
        if (!sourceFiles.any {
            it.absolutePath.endsWith(".kt")
        }) {
            return;
        }
        val mKotlinHome  = File(project.getBinDirPath(), "kt_home").apply { mkdirs() }
        val mClassOutput = File(project.getBinDirPath(), "classes").apply { mkdirs() }

        val collector = object : MessageCollector {
            private val diagnostics = mutableListOf<Diagnostic>()

            override fun clear() { diagnostics.clear() }

            override fun hasErrors() = diagnostics.any { it.severity.isError }

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                // do not add redundant logging messages
                if (severity != CompilerMessageSeverity.LOGGING) {
                    diagnostics += Diagnostic(severity, message, location)
                }
            }

            override fun toString() = diagnostics
                .joinToString(System.lineSeparator().repeat(2)) { it.toString() }
        }

        val arguments = mutableListOf<String>().apply {
            // Classpath
            add("-cp")
            add(
                    FileUtil.getClasspathDir() +
                    "android.jar" +
                    File.pathSeparator +
                    FileUtil.getClasspathDir() +
                    "core-lambda-stubs.jar" +
                    File.pathSeparator +
                    FileUtil.getClasspathDir() +
                    "kotlin-stdlib-1.7.10.jar"
                )

            // Sources (.java & .kt)
            add(project.getSrcDirPath())
        }

        val args = K2JVMCompilerArguments().apply {
            compileJava = true
            includeRuntime = false
            noJdk = true
            noReflect = true
            noStdlib = true
            kotlinHome = mKotlinHome.absolutePath
            destination = mClassOutput.absolutePath
        }

        val cacheDir = File(project.getBinDirPath(), "caches")

        IncrementalJvmCompilerRunner.makeIncrementally(cacheDir,
            listOf(File(project.getSrcDirPath()),
            args, collector)

        if (collector.hasErrors()) {
            throw CompilationFailedException(collector.toString())
        }
        File(mClassOutput, "META-INF").deleteRecursively()
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
    
    override fun getTaskName() : String {
        return "Kotlin Compiler"
    }

    private data class Diagnostic(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?
    )

}
