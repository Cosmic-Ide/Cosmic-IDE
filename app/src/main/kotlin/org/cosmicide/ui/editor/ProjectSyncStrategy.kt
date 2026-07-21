package org.cosmicide.ui.editor

import org.cosmicide.project.ProjectCommand
import org.cosmicide.project.ProjectCommandKind

/** Selects the one sync implementation owned by an editor project session. */
internal sealed interface ProjectSyncStrategy {
    data object GradleWrapper : ProjectSyncStrategy

    data class PluginCommand(val command: ProjectCommand) : ProjectSyncStrategy

    data object Unavailable : ProjectSyncStrategy
}

internal fun resolveProjectSyncStrategy(
    hasGradleWrapper: Boolean,
    projectCommands: List<ProjectCommand>
): ProjectSyncStrategy {
    if (hasGradleWrapper) return ProjectSyncStrategy.GradleWrapper

    return projectCommands
        .firstOrNull { it.kind == ProjectCommandKind.SYNC }
        ?.let(ProjectSyncStrategy::PluginCommand)
        ?: ProjectSyncStrategy.Unavailable
}
