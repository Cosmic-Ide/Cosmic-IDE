package org.cosmicide.project

import java.io.File

class Project(val root: File, val language: Language) {
    fun delete() {
        root.deleteRecursively()
    }

    val name: String = root.name

    val srcDir: () -> File
        get() = {
            if (language == Java) {
                File(root, "src/main/java")
            } else {
                File(root, "src/main/kotlin")
            }
        }

    val buildDir = File(root, "build")

    val cacheDir: File = File(buildDir, "cache")

    val binDir = File(buildDir, "bin")

    val libDir: File = File(root, "libs")

}