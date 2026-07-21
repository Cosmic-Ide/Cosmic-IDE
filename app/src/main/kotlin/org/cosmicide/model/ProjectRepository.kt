package org.cosmicide.model

import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.util.FileUtil
import java.io.File

interface ProjectRepository {
    fun projects(): List<Project>

    fun delete(project: Project)
}

internal class FileSystemProjectRepository(
    projectsDirectory: File = FileUtil.projectDir
) : ProjectRepository {
    private val root = projectsDirectory.canonicalFile

    override fun projects(): List<Project> = root
        .listFiles { file -> file.isDirectory }
        ?.sortedByDescending(File::lastModified)
        .orEmpty()
        .map { projectRoot ->
            Project(projectRoot, detectLanguage(projectRoot))
        }

    override fun delete(project: Project) {
        val target = project.root.canonicalFile
        require(target.parentFile == root) {
            "Projects can only be deleted from the configured projects directory"
        }
        require(target.isDirectory) { "Project no longer exists: ${project.name}" }
        check(target.deleteRecursively()) { "Could not delete ${project.name}" }
    }

    private fun detectLanguage(projectRoot: File): Language = when {
        hasSourceDirectory(projectRoot, "java") -> Language.Java
        hasSourceDirectory(projectRoot, "kotlin") -> Language.Kotlin
        hasSourceDirectory(projectRoot, "scala") -> Language.Scala
        else -> Language.Kotlin
    }

    private fun hasSourceDirectory(projectRoot: File, languageDirectory: String): Boolean {
        return projectRoot.resolve("src/main/$languageDirectory").isDirectory ||
                projectRoot.resolve("app/src/main/$languageDirectory").isDirectory
    }
}
