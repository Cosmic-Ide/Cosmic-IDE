/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.api

object PluginIds {
    const val CORE = "org.cosmicide.core"
}

data class PluginDependency(
    val id: String,
    val minVersion: String? = null,
    val optional: Boolean = false
)

data class PluginDescriptor(
    val id: String,
    val name: String,
    val version: String,
    val entryClass: String,
    val description: String = "",
    val author: String = "",
    val source: String = "",
    val classPath: List<String> = emptyList(),
    val dependencies: List<PluginDependency> = emptyList(),
    val capabilities: Set<String> = emptySet(),
    val enabledByDefault: Boolean = true
) {
    init {
        require(PLUGIN_ID.matches(id)) {
            "Plugin id '$id' must use reverse-domain characters: letters, numbers, '.', '_' or '-'"
        }
        require(name.isNotBlank()) { "Plugin name must not be blank" }
        require(version.isNotBlank()) { "Plugin version must not be blank" }
        require(entryClass.isNotBlank()) { "Plugin entry class must not be blank" }
    }

    companion object {
        private val PLUGIN_ID = Regex("[A-Za-z0-9_.-]+")
    }
}
