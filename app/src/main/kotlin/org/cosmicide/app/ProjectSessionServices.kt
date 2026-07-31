package org.cosmicide.app

import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCommand
import org.cosmicide.project.ProjectExtensionPoints
import org.cosmicide.project.ProjectTaskProvider

internal interface ProjectSessionServices {
    fun commands(project: Project): List<ProjectCommand>

    fun taskProviders(project: Project): List<ProjectTaskProvider>
}

internal object DefaultProjectSessionServices : ProjectSessionServices {
    override fun commands(project: Project): List<ProjectCommand> = CosmicPluginHost
        .enabledExtensions(ProjectExtensionPoints.COMMAND_PROVIDER)
        .flatMap { provider -> provider.commands(project) }

    override fun taskProviders(project: Project): List<ProjectTaskProvider> = CosmicPluginHost
        .enabledExtensions(ProjectExtensionPoints.TASK_PROVIDER)
        .filter { provider ->
            runCatching { provider.supports(project) }.getOrDefault(false)
        }

}
