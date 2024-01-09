/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.build.kotlin

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.build.util.getSourceFiles
import org.cosmicide.build.util.getSystemClasspath
import org.cosmicide.common.Prefs
import org.cosmicide.project.Project
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.makeJvmIncrementally
import java.io.File

class KotlinCompiler(val project: Project) : Task {

    val args: K2JVMCompilerArguments by lazy {
        K2JVMCompilerArguments().apply {
            noReflect = true
            noStdlib = true
            noJdk = true
            newInference = true
            useFirLT = true
            useFirIC = true
        }
    }

    override fun execute(reporter: BuildReporter) {
        val sourceFiles = project.srcDir.getSourceFiles("kt")
        if (sourceFiles.isEmpty()) {
            reporter.reportInfo("No Kotlin files are present. Skipping Kotlin compilation.")
            return
        }

        val kotlinHomeDir = project.binDir.resolve("kotlin").apply { mkdirs() }
        val classOutput = project.binDir.resolve("classes").apply { mkdirs() }
        val classpathFiles = collectClasspathFiles()

        val enabledPlugins = getKotlinCompilerPlugins().map(File::getAbsolutePath).toTypedArray()

        args.apply {
            classpath =
                (getSystemClasspath() + classpathFiles).joinToString(separator = File.pathSeparator) { it.absolutePath }
            kotlinHome = kotlinHomeDir.absolutePath
            destination = classOutput.absolutePath
            javaSourceRoots =
                sourceFiles.filter { it.isJavaFile() }.map { it.absolutePath }.toTypedArray()
            moduleName = project.name
            pluginClasspaths = enabledPlugins
            useFastJarFileSystem = Prefs.useFastJarFs
            languageVersion = Prefs.kotlinVersion
            apiVersion = Prefs.kotlinVersion
            jvmTarget = Prefs.compilerJavaVersion.toString()
            script = false
        }

        val collector = createMessageCollector(reporter)

        makeJvmIncrementally(kotlinHomeDir, listOf(project.srcDir), args, collector)
    }

    fun collectClasspathFiles(): List<File> {
        return project.libDir.walk().filter(File::isFile).toList()
    }

    fun getKotlinCompilerPlugins(): List<File> {
        val pluginDir = File(project.root, "kt_plugins")

        return pluginDir.walk().filter(File::isFile).toList()
    }

    fun createMessageCollector(reporter: BuildReporter): MessageCollector =
        object : MessageCollector {

            private var hasErrors: Boolean = false

            override fun clear() {}

            override fun hasErrors() = hasErrors

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                val diagnostic = CompilationDiagnostic(message, location)
                when (severity) {
                    CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> {
                        hasErrors = true
                        reporter.reportError(diagnostic.toString())
                    }

                    CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> reporter.reportWarning(
                        diagnostic.toString()
                    )

                    CompilerMessageSeverity.INFO -> reporter.reportInfo(diagnostic.toString())
                    CompilerMessageSeverity.LOGGING -> reporter.reportLogging(diagnostic.toString())
                    CompilerMessageSeverity.OUTPUT -> reporter.reportOutput(diagnostic.toString())
                }
            }
        }

    data class CompilationDiagnostic(
        val message: String,
        val location: CompilerMessageSourceLocation?
    ) {
        override fun toString() =
            location?.toString()?.substringAfter("src/main/").orEmpty() + " " + message
    }
}
