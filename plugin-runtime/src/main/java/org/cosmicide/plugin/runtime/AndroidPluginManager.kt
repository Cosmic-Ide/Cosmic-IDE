/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime

import android.content.Context
import android.util.Log
import org.cosmicide.plugin.api.CosmicPlugin
import org.cosmicide.plugin.api.DefaultServiceRegistry
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.MutableServiceRegistry
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.api.PluginHandle
import org.cosmicide.plugin.api.PluginLoadResult
import org.cosmicide.plugin.api.PluginManager
import org.cosmicide.plugin.api.PluginState
import org.cosmicide.plugin.runtime.loading.PluginClassLoaderFactory
import org.cosmicide.plugins.AndroidPluginServices
import java.io.File

class AndroidPluginManager(
    context: Context,
    private val extensionRegistry: MutableExtensionRegistry,
    private val pluginRoot: File,
    private val serviceRegistry: MutableServiceRegistry = DefaultServiceRegistry()
) : PluginManager {

    private val appContext = context.applicationContext
    private val classLoaderFactory = PluginClassLoaderFactory(appContext)
    private val activePlugins = LinkedHashMap<String, ActivePlugin>()
    private val handles = LinkedHashMap<String, PluginHandle>()

    override val plugins: List<PluginHandle>
        get() = synchronized(handles) { handles.values.toList() }

    init {
        pluginRoot.mkdirs()
        serviceRegistry.register(AndroidPluginServices.APPLICATION_CONTEXT, appContext)
    }

    /**
     * Metadata-first scan: every visible package directory is read, incompatible packages get a
     * FAILED handle, and one malformed package does not abort discovery of the remaining ones.
     * Validation here still executes no plugin code; only load() activates classes.
     */
    fun loadInstalledPlugins(): List<PluginLoadResult> {
        return PluginDiscovery.discover(pluginRoot).map { result ->
            when (result) {
                is PluginDiscoveryResult.Valid -> {
                    // Incompatible declared ranges fail here with a specific reason and no
                    // class loader is created, so no plugin code executes.
                    val compatibilityError =
                        runCatching { result.metadata.requireCompatible() }.exceptionOrNull()
                    if (compatibilityError == null) {
                        load(result.metadata.descriptor, result.directory)
                    } else {
                        updateHandle(
                            result.metadata.descriptor,
                            PluginState.FAILED,
                            compatibilityError.message
                        )
                        PluginLoadResult.Failed(
                            result.metadata.descriptor,
                            compatibilityError.message ?: "incompatible plugin API",
                            compatibilityError
                        )
                    }
                }
                is PluginDiscoveryResult.Invalid -> {
                    Log.w(TAG, "Skipping ${result.directory.name}: ${result.reason}", result.cause)
                    // The descriptor is a diagnostic-only handle; the raw directory name is
                    // sanitized to the plugin id grammar.
                    val invalidId = result.directory.name
                        .map { if (it.isLetterOrDigit() || it in "._-") it else '_' }
                        .joinToString("")
                    PluginLoadResult.Failed(
                        PluginDescriptor(
                            id = "org.cosmicide.invalid.$invalidId",
                            name = result.directory.name,
                            version = "0.0.0",
                            entryClass = "unknown",
                            source = "invalid"
                        ),
                        reason = result.reason,
                        cause = result.cause
                    )
                }
            }
        }
    }

    override fun load(descriptor: PluginDescriptor): PluginLoadResult {
        // Confine the id-derived directory and re-read the persisted manifest before trusting
        // the caller-provided descriptor.
        val metadata = try {
            validateInstalledPlugin(pluginRoot, descriptor)
        } catch (error: IllegalArgumentException) {
            updateHandle(descriptor, PluginState.FAILED, error.message)
            return PluginLoadResult.Failed(descriptor, error.message ?: "invalid installed plugin", error)
        }
        return load(metadata.descriptor, installedPluginDirectory(pluginRoot, metadata.descriptor.id))
    }

    /** Activates an app-bundled plugin through the same context and lifecycle as installed plugins. */
    fun loadBuiltin(descriptor: PluginDescriptor, plugin: CosmicPlugin): PluginLoadResult {
        if (!descriptor.enabledByDefault) {
            updateHandle(descriptor, PluginState.DISABLED)
            return PluginLoadResult.Failed(descriptor, "Plugin is disabled by default")
        }

        activePlugins[descriptor.id]?.let {
            updateHandle(
                descriptor,
                PluginState.ACTIVE,
                setupActions = it.plugin.setupActions
            )
            return PluginLoadResult.Loaded(descriptor, it.plugin)
        }

        updateHandle(descriptor, PluginState.DISCOVERED)
        var pluginContext: DefaultPluginContext? = null

        return try {
            pluginContext = DefaultPluginContext(
                descriptor = descriptor,
                extensions = extensionRegistry,
                services = serviceRegistry,
                logger = AndroidPluginLogger(descriptor.id)
            )
            plugin.activate(pluginContext)

            activePlugins[descriptor.id] = ActivePlugin(
                descriptor = descriptor,
                plugin = plugin,
                context = pluginContext,
                classLoader = plugin.javaClass.classLoader ?: javaClass.classLoader
            )
            updateHandle(
                descriptor,
                PluginState.ACTIVE,
                setupActions = plugin.setupActions
            )
            PluginLoadResult.Loaded(descriptor, plugin)
        } catch (throwable: Throwable) {
            pluginContext?.disposeAll()
            extensionRegistry.unregisterOwner(descriptor.id)
            updateHandle(descriptor, PluginState.FAILED, throwable.message)

            throwable.printStackTrace()

            PluginLoadResult.Failed(
                descriptor = descriptor,
                reason = throwable.message ?: "Plugin activation failed",
                cause = throwable
            )
        }
    }

    private fun load(descriptor: PluginDescriptor, pluginDir: File): PluginLoadResult {
        if (!descriptor.enabledByDefault) {
            updateHandle(descriptor, PluginState.DISABLED)
            return PluginLoadResult.Failed(descriptor, "Plugin is disabled by default")
        }

        activePlugins[descriptor.id]?.let {
            updateHandle(
                descriptor,
                PluginState.ACTIVE,
                setupActions = it.plugin.setupActions
            )
            return PluginLoadResult.Loaded(descriptor, it.plugin)
        }

        updateHandle(descriptor, PluginState.DISCOVERED)
        var pluginContext: DefaultPluginContext? = null

        return try {
            val classLoader = classLoaderFactory.create(pluginDir, descriptor)
            val plugin = classLoader
                .loadClass(descriptor.entryClass)
                .getDeclaredConstructor()
                .newInstance() as CosmicPlugin

            pluginContext = DefaultPluginContext(
                descriptor = descriptor,
                extensions = extensionRegistry,
                services = serviceRegistry.copy()
                    .apply { register(AndroidPluginServices.PLUGIN_DIRECTORY, pluginDir) },
                logger = AndroidPluginLogger(descriptor.id)
            )
            plugin.activate(pluginContext)

            activePlugins[descriptor.id] = ActivePlugin(
                descriptor = descriptor,
                plugin = plugin,
                context = pluginContext,
                classLoader = classLoader
            )
            updateHandle(
                descriptor,
                PluginState.ACTIVE,
                setupActions = plugin.setupActions
            )
            PluginLoadResult.Loaded(descriptor, plugin)
        } catch (throwable: Throwable) {
            pluginContext?.disposeAll()
            extensionRegistry.unregisterOwner(descriptor.id)
            updateHandle(descriptor, PluginState.FAILED, throwable.message)

            throwable.printStackTrace()

            PluginLoadResult.Failed(
                descriptor = descriptor,
                reason = throwable.message ?: "Plugin activation failed",
                cause = throwable
            )
        }
    }

    override fun unload(pluginId: String) {
        val active = activePlugins.remove(pluginId) ?: return
        runCatching {
            active.plugin.deactivate()
        }.onFailure {
            active.context.logger.warn("Plugin deactivation failed", it)
        }
        active.context.disposeAll()
        extensionRegistry.unregisterOwner(pluginId)
        updateHandle(
            active.descriptor,
            PluginState.DISABLED,
            setupActions = active.plugin.setupActions
        )
    }

    /**
     * Removes a fully unloaded plugin from the runtime's discovered plugin list.
     * Package deletion remains the responsibility of the marketplace installer.
     */
    fun forget(pluginId: String) {
        check(pluginId !in activePlugins) { "Plugin $pluginId must be unloaded before removal" }
        synchronized(handles) {
            handles.remove(pluginId)
        }
    }

    private fun updateHandle(
        descriptor: PluginDescriptor,
        state: PluginState,
        errorMessage: String? = null,
        setupActions: List<org.cosmicide.plugin.api.PluginSetupAction> = emptyList()
    ) {
        synchronized(handles) {
            handles[descriptor.id] = PluginHandle(
                descriptor = descriptor,
                state = state,
                errorMessage = errorMessage,
                setupActions = setupActions
            )
        }
    }

    private data class ActivePlugin(
        val descriptor: PluginDescriptor,
        val plugin: CosmicPlugin,
        val context: DefaultPluginContext,
        val classLoader: ClassLoader
    )

    private companion object {
        const val TAG = "AndroidPluginManager"
    }
}
