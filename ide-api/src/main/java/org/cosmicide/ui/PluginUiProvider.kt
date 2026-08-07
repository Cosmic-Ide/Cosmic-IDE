/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.ui

import androidx.compose.runtime.Composable
import org.cosmicide.plugin.api.ConfigurableExtension

/**
 * Base interface for plugin-contributed UI.
 */
interface PluginUiProvider : ConfigurableExtension {
    @Composable
    fun Content()
}

/**
 * An extension point for plugins to contribute their own settings UI.
 */
interface SettingsUiProvider : PluginUiProvider {
    /**
     * The label to show in the settings list.
     */
    val label: String
}

/**
 * An extension point for plugins to contribute their own full-screen UI.
 */
interface PluginScreenProvider : PluginUiProvider {
    /**
     * The unique ID for this screen within the plugin.
     */
    val screenId: String

    /**
     * The title to show in the app bar.
     */
    val title: String
}
