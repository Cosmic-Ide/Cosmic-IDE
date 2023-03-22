package org.cosmicide.rewrite.compile

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.dex.D8Task
import org.cosmicide.build.java.JavaCompileTask
import org.cosmicide.build.kotlin.KotlinCompiler
import org.cosmicide.project.Project

class Compiler(
    private val project: Project
) {

    private val compilerCache =
        CompilerCache(JavaCompileTask(project), KotlinCompiler(project), D8Task(project))

    fun compile(reporter: BuildReporter) {
        reporter.reportInfo("Compiling Kotlin")
        compilerCache.kotlinCompiler.execute(reporter)
        reporter.reportInfo("Finished Compiling Kotlin")
        reporter.reportInfo("Compiling Java")
        compilerCache.javaCompiler.execute(reporter)
        reporter.reportInfo("Finished Compiling Java")
        reporter.reportInfo("Compiling class files to dex")
        compilerCache.dexTask.execute(reporter)
        reporter.reportInfo("Finished Compiling class files to dex")
        reporter.reportSuccess()
    }
}