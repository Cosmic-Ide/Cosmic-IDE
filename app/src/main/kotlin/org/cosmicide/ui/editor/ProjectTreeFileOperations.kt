package org.cosmicide.ui.editor

import java.io.File

/** Owns validated filesystem mutations initiated by the project tree. */
internal class ProjectTreeFileOperations(rootDirectory: File) {
    private val root = rootDirectory.canonicalFile

    fun create(
        parentDirectory: File,
        name: String,
        suffix: String = "",
        directory: Boolean = false
    ): File {
        val parent = requireInsideRoot(parentDirectory, allowRoot = true)
        require(parent.isDirectory) { "The destination folder no longer exists" }
        val target = resolveChild(parent, validatedName(name) + suffix)
        require(!target.exists()) { "${target.name} already exists" }

        val created = if (directory) target.mkdir() else target.createNewFile()
        check(created) { "Could not create ${target.name}" }
        return target
    }

    fun rename(targetFile: File, newName: String): File {
        val target = requireInsideRoot(targetFile)
        require(target.exists()) { "${target.name} no longer exists" }
        val parent = target.parentFile ?: error("The project root cannot be renamed")
        val renamed = resolveChild(parent, validatedName(newName))
        require(!renamed.exists()) { "${renamed.name} already exists" }
        check(target.renameTo(renamed)) { "Could not rename ${target.name}" }
        return renamed
    }

    fun delete(targetFile: File) {
        val target = requireInsideRoot(targetFile)
        require(target.exists()) { "${target.name} no longer exists" }
        check(target.deleteRecursively()) { "Could not delete ${target.name}" }
    }

    private fun validatedName(rawName: String): String {
        val name = rawName.trim()
        require(name.isNotEmpty()) { "Name is required" }
        require(name != "." && name != "..") { "Invalid name" }
        require('/' !in name && '\\' !in name) { "Name cannot contain path separators" }
        return name
    }

    private fun resolveChild(parent: File, name: String): File {
        val target = parent.resolve(name).canonicalFile
        require(target.parentFile == parent.canonicalFile) {
            "The destination must remain inside its current folder"
        }
        requireInsideRoot(target)
        return target
    }

    private fun requireInsideRoot(file: File, allowRoot: Boolean = false): File {
        val target = file.canonicalFile
        val insideRoot = target.toPath().startsWith(root.toPath())
        require(insideRoot && (allowRoot || target != root)) {
            "Project tree operations must remain inside the project"
        }
        return target
    }
}
