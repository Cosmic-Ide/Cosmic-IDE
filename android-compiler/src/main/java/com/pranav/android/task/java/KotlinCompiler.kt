package com.pranav.android.task.java

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.makeIncrementally
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
            it.endsWith(".kt")
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

        val args = K2JVMCompilerArguments().apply {
            useJavac = false
            compileJava = false
            includeRuntime = false
            noJdk = true
            noReflect = true
            noStdlib = true
            classpath =
                    FileUtil.getClasspathDir() +
                    "android.jar" +
                    File.pathSeparator +
                    FileUtil.getClasspathDir() +
                    "core-lambda-stubs.jar" +
                    File.pathSeparator +
                    FileUtil.getClasspathDir() +
                    "kotlin-stdlib-1.7.10.jar"
            kotlinHome = mKotlinHome.absolutePath
            destination = mClassOutput.absolutePath
            javaSourceRoots = sourceFiles.filter {
                it.endsWith(".java")
            }.toTypedArray()
            // incremental compiler needs this somewhy
            moduleName = "project-kotlin"
        }

        val cacheDir = File(project.getBinDirPath(), "caches")

        makeIncrementally(
                cacheDir,
                listOf(File(project.getSrcDirPath())),
                args,
                collector
        )

        if (collector.hasErrors()) {
            throw CompilationFailedException(collector.toString())
        }
        // File(mClassOutput, "META-INF").deleteRecursively()
    }

    fun getSourceFiles(path: File): ArrayList<String> {
        val sourceFiles = arrayListOf<String>()
        val files = path.listFiles()
        if (files == null) {
            return arrayListOf<String>()
        }
        for (file in files) {
            if (file.isFile()) {
                val path = file.absolutePath
                if (path.endsWith(".java") || path.endsWith(".kt")) {
                    sourceFiles.add(path)
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