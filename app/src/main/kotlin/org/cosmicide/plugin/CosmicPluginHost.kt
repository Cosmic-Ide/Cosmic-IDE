/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin

import android.content.Context
import android.util.Log
import org.cosmicide.editor.language.registerBuiltinEditorExtensions
import org.cosmicide.ide.editor.EditorExtensionPoints
import org.cosmicide.plugin.api.ConfigurableExtension
import org.cosmicide.plugin.api.DefaultExtensionRegistry
import org.cosmicide.plugin.api.DefaultServiceRegistry
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.runtime.AndroidPluginManager
import org.cosmicide.util.FileUtil

object CosmicPluginHost {

    val extensionRegistry: MutableExtensionRegistry = DefaultExtensionRegistry()
    private val serviceRegistry = DefaultServiceRegistry()

    lateinit var extensionSettings: ExtensionSettings
        private set

    @Volatile
    private var initialized = false

    var pluginManager: AndroidPluginManager? = null
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            extensionSettings = ExtensionSettings(context)
            registerBuiltinEditorExtensions(context, extensionRegistry)
            pluginManager = AndroidPluginManager(
                context = context,
                extensionRegistry = extensionRegistry,
                pluginRoot = FileUtil.pluginDir,
                serviceRegistry = serviceRegistry
            ).also { manager ->
                manager.loadInstalledPlugins().forEach { result ->
                    result.onFailure { descriptorId, reason, throwable ->
                        Log.w(TAG, "Failed to load plugin $descriptorId: $reason", throwable)
                    }
                }
            }

            initialized = true
        }
    }

    fun <T> enabledExtensions(point: org.cosmicide.plugin.api.ExtensionPoint<T>): List<T>
            where T : Any, T : ConfigurableExtension {
        return extensionRegistry.extensions(point).filter(extensionSettings::isEnabled)
    }

    fun configurableExtensions(): List<ExtensionSettingsItem> {
        return buildList {
            addRegistrations(EditorExtensionPoints.LANGUAGE_PROVIDER, "Editor languages")
            addRegistrations(EditorExtensionPoints.LSP_SERVER_PROVIDER, "Language servers")
            addRegistrations(EditorExtensionPoints.FORMATTER_PROVIDER, "Formatters")
        }.distinctBy { it.extension.id }
    }

    private fun <T> MutableList<ExtensionSettingsItem>.addRegistrations(
        point: org.cosmicide.plugin.api.ExtensionPoint<T>,
        category: String
    ) where T : Any, T : ConfigurableExtension {
        extensionRegistry.registrations(point)
            .filter { it.extension.canDisable }
            .mapTo(this) {
                ExtensionSettingsItem(it.extension, it.ownerPluginId, category)
            }
    }

    private inline fun org.cosmicide.plugin.api.PluginLoadResult.onFailure(
        block: (descriptorId: String, reason: String, throwable: Throwable?) -> Unit
    ) {
        if (this is org.cosmicide.plugin.api.PluginLoadResult.Failed) {
            block(descriptor.id, reason, cause)
        }
    }

    private const val TAG = "CosmicPluginHost"
}
