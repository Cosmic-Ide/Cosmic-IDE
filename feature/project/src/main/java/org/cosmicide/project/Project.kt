/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.project

import java.io.File
import java.io.Serializable

/**
 * Represents a project.
 *
 * @property root The root directory of the project.
 * @property language The programming language used in the project.
 */
data class Project(
    val root: File,
    val language: Language
) : Serializable {

    /**
     * The name of the project, derived from the root directory.
     */
    val name: String = root.name

    /**
     * The source directory of the project, based on the language used.
     */
    val srcDir: File
        get() = when (language) {
            is Language.Java -> File(root, "src/main/java")
            is Language.Kotlin -> File(root, "src/main/kotlin")
        }

    /**
     * The build directory of the project.
     */
    val buildDir = File(root, "build")

    /**
     * The cache directory of the project.
     */
    val cacheDir = File(buildDir, "cache")

    /**
     * The binary directory of the project.
     */
    val binDir = File(buildDir, "bin")

    /**
     * The library directory of the project.
     */
    val libDir = File(root, "libs")

    /**
     * Deletes the project directory.
     *
     * @throws IllegalStateException if the root directory is not a valid project directory.
     */
    fun delete() {
        if (root.isDirectory && root.name == name) {
            root.deleteRecursively()
        } else {
            throw IllegalStateException("Cannot delete directory: ${root.absolutePath}")
        }
    }
}