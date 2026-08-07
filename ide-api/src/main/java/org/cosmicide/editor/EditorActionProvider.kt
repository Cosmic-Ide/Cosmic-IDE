/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor

import org.cosmicide.plugin.api.ConfigurableExtension
import org.cosmicide.project.Project
import java.io.File

/**
 * An extension point for plugins to contribute their own actions to the editor toolbar menu.
 */
interface EditorActionProvider : ConfigurableExtension {
    /**
     * Returns a list of actions to show in the editor toolbar menu.
     */
    fun actions(project: Project, file: File?): List<EditorAction>
}

/**
 * Represents a single action contributed by a plugin.
 */
data class EditorAction(
    val id: String,
    val label: String,
    val description: String = "",
    val icon: String? = null,
    val onClick: (EditorActionContext) -> Unit
)

/**
 * Context provided to plugin actions when they are clicked.
 */
interface EditorActionContext {
    val project: Project
    val file: File?

    /**
     * Navigates to a screen provided by the same plugin.
     */
    fun navigateToPluginScreen(screenId: String, args: Map<String, String> = emptyMap())
}
