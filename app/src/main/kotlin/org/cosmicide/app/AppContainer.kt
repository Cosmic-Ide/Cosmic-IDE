package org.cosmicide.app

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.cosmicide.model.FileSystemProjectRepository
import org.cosmicide.model.ProjectRepository
import org.cosmicide.model.ProjectViewModel
import org.cosmicide.ui.home.AndroidHomeProjectArchiveRepository
import org.cosmicide.ui.home.HomeExtensionRepository
import org.cosmicide.ui.home.HomeProjectArchiveRepository
import org.cosmicide.ui.home.PluginHomeExtensionRepository
import org.cosmicide.ui.project.AndroidGradleProjectCreator
import org.cosmicide.ui.project.GradleProjectCreator
import org.cosmicide.ui.settings.extensions.AndroidExtensionsSettingsRepository
import org.cosmicide.ui.settings.extensions.ExtensionsSettingsRepository
import org.cosmicide.util.FileUtil
import java.io.File

/**
 * Activity-scoped composition root for services still implemented in the app module.
 *
 * Features consume the contracts exposed here. When a feature moves to its own module, its
 * Android implementation can move without changing the screen's state and rendering layers.
 */
internal class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val projectsDirectory: File = FileUtil.projectDir
    val projectRepository: ProjectRepository = FileSystemProjectRepository(projectsDirectory)
    val projectViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { ProjectViewModel(projectRepository) }
    }
    val projectSessionServices: ProjectSessionServices = DefaultProjectSessionServices
    val homeProjectArchiveRepository: HomeProjectArchiveRepository =
        AndroidHomeProjectArchiveRepository(appContext, projectsDirectory)
    val homeExtensionRepository: HomeExtensionRepository = PluginHomeExtensionRepository
    val extensionsSettingsRepository: ExtensionsSettingsRepository =
        AndroidExtensionsSettingsRepository(appContext)
    val gradleProjectCreator: GradleProjectCreator =
        AndroidGradleProjectCreator(appContext, projectsDirectory)
}

internal val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer has not been provided")
}
