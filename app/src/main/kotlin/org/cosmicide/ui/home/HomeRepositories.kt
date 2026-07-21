package org.cosmicide.ui.home

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectActionProvider
import org.cosmicide.project.ProjectCreationProvider
import org.cosmicide.project.ProjectExtensionPoints
import org.cosmicide.util.FileUtil
import org.cosmicide.util.compressToZip
import org.cosmicide.util.unzip
import java.io.File

internal interface HomeProjectArchiveRepository {
    val projectsDirectory: File

    suspend fun backup(project: Project, destination: Uri)

    suspend fun importArchive(source: Uri): File
}

internal class AndroidHomeProjectArchiveRepository(
    context: Context,
    override val projectsDirectory: File = FileUtil.projectDir
) : HomeProjectArchiveRepository {
    private val appContext = context.applicationContext

    override suspend fun backup(project: Project, destination: Uri) =
        withContext(Dispatchers.IO) {
            val output = appContext.contentResolver.openOutputStream(destination)
                ?: error("Could not open the backup destination")
            output.use(project.root::compressToZip)
        }

    override suspend fun importArchive(source: Uri): File = withContext(Dispatchers.IO) {
        val archiveName = DocumentFile.fromSingleUri(appContext, source)?.name
            ?: error("Could not determine the archive name")
        val projectName = projectNameFromArchiveName(archiveName)
        val root = projectsDirectory.canonicalFile
        val target = root.resolve(projectName).canonicalFile
        require(target.parentFile == root) { "Invalid project archive name" }
        require(!target.exists()) { "Project already exists" }
        check(target.mkdir()) { "Could not create the project directory" }

        try {
            val input = appContext.contentResolver.openInputStream(source)
                ?: error("Could not open the project archive")
            input.use { it.unzip(target) }
            target
        } catch (error: Throwable) {
            target.deleteRecursively()
            throw error
        }
    }
}

internal fun projectNameFromArchiveName(archiveName: String): String {
    val name = archiveName.trim().removeSuffix(".zip").trim()
    require(name.isNotEmpty() && name != "." && name != "..") {
        "Archive must have a project name"
    }
    require('/' !in name && '\\' !in name) { "Invalid project archive name" }
    return name
}

internal interface HomeExtensionRepository {
    fun creationProviders(): List<ProjectCreationProvider>

    fun actionProviders(): List<ProjectActionProvider>
}

internal object PluginHomeExtensionRepository : HomeExtensionRepository {
    override fun creationProviders(): List<ProjectCreationProvider> = CosmicPluginHost
        .enabledExtensions(ProjectExtensionPoints.CREATION_PROVIDER)
        .filter(ProjectCreationProvider::isAvailable)

    override fun actionProviders(): List<ProjectActionProvider> = CosmicPluginHost
        .enabledExtensions(ProjectExtensionPoints.ACTION_PROVIDER)
}
