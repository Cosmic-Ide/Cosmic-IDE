/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.api

interface CosmicPlugin {
    /** Interactive environment setup commands owned by this plugin as a whole. */
    val setupActions: List<PluginSetupAction>
        get() = emptyList()

    fun activate(context: PluginContext)

    fun deactivate() = Unit
}

data class PluginSetupAction(
    val id: String,
    val label: String,
    val command: String,
    val description: String = ""
) {
    init {
        require(id.isNotBlank()) { "Plugin setup action id must not be blank" }
        require(label.isNotBlank()) { "Plugin setup action label must not be blank" }
        require(command.isNotBlank()) { "Plugin setup command must not be blank" }
    }
}
