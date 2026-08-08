package org.cosmicide.model

import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectTypeProvider
import org.cosmicide.util.FileUtil
import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface ProjectRepository {
    fun projects(): List<Project>

    fun delete(project: Project)
}

internal class FileSystemProjectRepository(
    projectsDirectory: File = FileUtil.projectDir,
    private val projectTypeProviders: () -> List<ProjectTypeProvider> = { emptyList() }
) : ProjectRepository {
    private val root = projectsDirectory.canonicalFile
    private val languageCache = ConcurrentHashMap<String, Language>()

    override fun projects(): List<Project> = root
        .listFiles { file -> file.isDirectory }
        ?.sortedByDescending(File::lastModified)
        .orEmpty()
        .map { projectRoot ->
            Project(projectRoot, languageCache.getOrPut(projectRoot.absolutePath) {
                detectLanguage(projectRoot)
            })
        }

    override fun delete(project: Project) {
        val target = project.root.canonicalFile
        require(target.parentFile == root) {
            "Projects can only be deleted from the configured projects directory"
        }
        require(target.isDirectory) { "Project no longer exists: ${project.name}" }
        languageCache.remove(project.root.absolutePath)
        check(target.deleteRecursively()) { "Could not delete ${project.name}" }
    }

    private fun detectLanguage(projectRoot: File): Language {
        projectTypeProviders().firstNotNullOfOrNull { provider ->
            runCatching {
                provider.takeIf { it.supports(projectRoot) }
                    ?.project(projectRoot)
                    ?.language
            }.getOrNull()
        }?.let { language ->
            return language
        }

        return when {
            else -> Language.Empty
        }
    }
}
