package org.cosmicide.build.java

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.project.Project
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * A task that creates a JAR file containing all the class files in a directory.
 */
class JarTask(val project: Project) : Task {

    override fun execute(reporter: BuildReporter) {
        val directory = project.binDir.resolve("classes")
        reporter.reportInfo("Creating JAR file from directory: ${directory.absolutePath}")

        val jarFile = File(project.binDir, "classes.jar")
        if (jarFile.exists()) {
            jarFile.delete()
        }

        JarOutputStream(FileOutputStream(jarFile)).use { jar ->
            directory.walkTopDown().filter { it.isFile && it.extension == "class" }
                .forEach { classFile ->
                    val entryName = classFile.relativeTo(directory).path.replace("\\", "/")
                    jar.putNextEntry(ZipEntry(entryName))
                    classFile.inputStream().use { input ->
                        input.copyTo(jar)
                    }
                    jar.closeEntry()
                    reporter.reportInfo("Added $entryName to JAR")
                }
        }

        reporter.reportInfo("JAR file created: ${jarFile.absolutePath}")
    }
}
