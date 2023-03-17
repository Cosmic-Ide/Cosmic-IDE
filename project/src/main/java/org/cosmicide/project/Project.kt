package org.cosmicide.project

import java.io.File
import java.io.Serializable

/**
 * A data class representing a project.
 */
data class Project(val root: File, val language: Language) : Serializable {

    val name: String = root.name

    /**
     * Returns the source directory of the project based on the language used.
     *
     * @return the source directory as a [File]
     */
    val srcDir: () -> File
        get() = {
            when (language) {
                is Java -> File(root, "src/main/java")
                is Kotlin -> File(root, "src/main/kotlin")
            }
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
     */
    fun delete() {
        root.deleteRecursively()
    }
}