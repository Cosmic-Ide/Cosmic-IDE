package org.cosmicide.app

import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCommand
import org.cosmicide.project.ProjectExtensionPoints
import org.cosmicide.tooling.ToolingServerManager

internal interface ProjectSessionServices {
    fun commands(project: Project): List<ProjectCommand>

    fun stopTooling()
}

internal object DefaultProjectSessionServices : ProjectSessionServices {
    override fun commands(project: Project): List<ProjectCommand> = CosmicPluginHost
        .enabledExtensions(ProjectExtensionPoints.COMMAND_PROVIDER)
        .flatMap { provider -> provider.commands(project) }

    override fun stopTooling() {
        ToolingServerManager.stopCurrent()
    }
}
