package org.cosmicide.rewrite.compile

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.java.JavaCompileTask
import org.cosmicide.build.kotlin.KotlinCompiler
import org.cosmicide.project.Project

class Compiler(
    private val project: Project
) {

    private val compilerCache = CompilerCache(JavaCompileTask(project), KotlinCompiler(project))

    fun compile(reporter: BuildReporter) {
        reporter.reportInfo("Compiling Kotlin")
        KotlinCompiler(project).execute(reporter)
        reporter.reportInfo("Finished Compiling Kotlin")
        reporter.reportInfo("Compiling Java")
        JavaCompileTask(project).execute(reporter)
        reporter.reportInfo("Finished Compiling Java")
    }
}