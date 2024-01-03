/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.build.java

import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.project.Project
import java.io.File
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

        JarOutputStream(jarFile.outputStream()).use { jar ->
            directory.walkTopDown().filter { it.isFile && it.extension == "class" }
                .forEach { classFile ->
                    val entryName = classFile.relativeTo(directory).path.replace("\\", "/")
                    jar.putNextEntry(ZipEntry(entryName))
                    classFile.inputStream().buffered().use { input ->
                        input.copyTo(jar)
                    }
                    jar.closeEntry()
                    reporter.reportInfo("Added $entryName to JAR")
                }
        }

        reporter.reportInfo("JAR file created: ${jarFile.absolutePath}")
    }
}
