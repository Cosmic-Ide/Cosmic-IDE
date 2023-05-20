package org.cosmicide.project

import java.io.File

/**
 * Represents a project.
 *
 * @property root The root directory of the project.
 * @property language The programming language used in the project.
 */
data class Project(
    val root: File,
    val language: Language
) {

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