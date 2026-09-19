/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.project

import org.cosmicide.plugin.api.Disposable
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.register
import java.io.File

/**
 * Helper for registering a custom project type matching a marker file or custom condition.
 */
fun PluginContext.registerProjectType(
    id: String,
    displayName: String,
    languageName: String,
    fileExtension: String,
    markerFile: String? = null,
    supportsPredicate: (File) -> Boolean = { markerFile != null && it.resolve(markerFile).isFile },
    priority: Int = 350
): Disposable {
    val provider = object : ProjectTypeProvider {
        override val id: String = id
        override val displayName: String = displayName
        override val languageName: String = languageName
        override val fileExtension: String = fileExtension
        override fun supports(projectRoot: File): Boolean = supportsPredicate(projectRoot)
    }
    return register(ProjectExtensionPoints.TYPE_PROVIDER, provider, priority)
}

/**
 * Helper for registering project-level commands.
 */
fun PluginContext.registerProjectCommands(
    id: String,
    displayName: String,
    priority: Int = 350,
    builder: (Project) -> List<ProjectCommand>
): Disposable {
    val provider = object : ProjectCommandProvider {
        override val id: String = id
        override val displayName: String = displayName
        override fun commands(project: Project): List<ProjectCommand> = builder(project)
    }
    return register(ProjectExtensionPoints.COMMAND_PROVIDER, provider, priority)
}
