package org.cosmicide.build.java

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.project.Project
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * A task that creates a JAR file containing all the class files in a directory.
 */
class JarTask(val project: Project) : Task {

    override fun execute(reporter: BuildReporter) {
        val directory = project.binDir.resolve("classes")
        reporter.reportInfo("Creating JAR file from directory: ${directory.absolutePath}")

        val jarFile = project.binDir.resolve("classes.jar").toFile()
        if (jarFile.exists()) {
            jarFile.delete()
        }

        JarOutputStream(FileOutputStream(jarFile)).use { jar ->
            Files.walk(directory.toPath()).use { paths ->
                paths.filter { it.toFile().isFile && it.toString().endsWith(".class") }
                    .forEach { classFilePath ->
                        val entryName = directory.toPath().relativize(classFilePath).toString().replace("\\", "/")
                        jar.putNextEntry(ZipEntry(entryName))

                        Files.copy(classFilePath, jar)
                        jar.closeEntry()

                        reporter.reportInfo("Added $entryName to JAR")
                    }
            }
        }

        reporter.reportInfo("JAR file created: ${jarFile.absolutePath}")
    }
}
