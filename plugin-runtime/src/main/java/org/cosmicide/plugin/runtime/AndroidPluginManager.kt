/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime

import android.content.Context
import org.cosmicide.plugin.api.CosmicPlugin
import org.cosmicide.plugin.api.DefaultServiceRegistry
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.MutableServiceRegistry
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.api.PluginHandle
import org.cosmicide.plugin.api.PluginLoadResult
import org.cosmicide.plugin.api.PluginManager
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
    private val runtime = PluginRuntimeController(extensionRegistry)

    override val plugins: List<PluginHandle> get() = runtime.plugins

    /** Conflated immutable diagnostics. Existing synchronous APIs keep their callback thread. */
    val states get() = runtime.states

    /** Opt-in IO-dispatched lifecycle operations; do not call or await from plugin callbacks. */
    suspend fun loadAsync(descriptor: PluginDescriptor): PluginLoadResult =
        runtime.executeAsync { load(descriptor) }

    suspend fun loadInstalledPluginsAsync(): List<PluginLoadResult> =
        runtime.executeAsync { loadInstalledPlugins() }

    suspend fun loadBuiltinsAsync(plugins: List<Pair<PluginDescriptor, CosmicPlugin>>): List<PluginLoadResult> =
        runtime.executeAsync { loadBuiltins(plugins) }

    suspend fun unloadAsync(pluginId: String) = runtime.unloadAsync(pluginId)

    init {
        pluginRoot.mkdirs()
        serviceRegistry.register(AndroidPluginServices.APPLICATION_CONTEXT, appContext)
    }

    /** Includes filesystem replacement in the lifecycle gate; never invoke from plugin callbacks. */
    fun <T> withPackageTransaction(operation: () -> T): T = runtime.exclusive(operation)

    /** Discover the entire installed set before any code is started. */
    fun loadInstalledPlugins(): List<PluginLoadResult> = runtime.exclusive {
        val discovery = discover()
        discovery.map { result ->
            when (result) {
                is PluginDiscoveryResult.Valid -> runtime.load(result.metadata.descriptor.id)
                is PluginDiscoveryResult.Invalid -> runtime.recordFailure(
                    invalidPluginDescriptor(
                        result.directory
                    ), result.cause
                )
            }
        }
    }

    override fun load(descriptor: PluginDescriptor): PluginLoadResult = runtime.exclusive {
        val metadata = try {
            validateInstalledPlugin(pluginRoot, descriptor)
        } catch (error: Exception) {
            return@exclusive runtime.recordFailure(descriptor, error)
        }
        discover()
        registerInstalled(metadata, installedPluginDirectory(pluginRoot, descriptor.id))
        runtime.load(descriptor.id)
    }

    /** The singular entry point remains ABI-compatible. Hosts may batch to resolve forward edges. */
    fun loadBuiltin(descriptor: PluginDescriptor, plugin: CosmicPlugin): PluginLoadResult =
        loadBuiltins(listOf(descriptor to plugin)).single()

    fun loadBuiltins(plugins: List<Pair<PluginDescriptor, CosmicPlugin>>): List<PluginLoadResult> =
        runtime.exclusive {
            discover()
            plugins.forEach { (descriptor, plugin) ->
                runtime.register(
                    PluginRuntimeController.Candidate(
                        descriptor,
                        "builtin:${descriptor.entryClass}",
                        create = { plugin },
                        context = {
                            DefaultPluginContext(
                                descriptor,
                                extensionRegistry,
                                serviceRegistry,
                                AndroidPluginLogger(descriptor.id)
                            )
                        })
                )
        }
            plugins.sortedBy { it.first.id }.map { runtime.load(it.first.id) }
        }

    override fun unload(pluginId: String) = runtime.unload(pluginId)

    /** Package deletion belongs to the marketplace, after a successful unload. */
    fun forget(pluginId: String) = runtime.forget(pluginId)

    private fun discover(): List<PluginDiscoveryResult> =
        PluginDiscovery.discover(pluginRoot).also { results ->
            results.filterIsInstance<PluginDiscoveryResult.Valid>().forEach {
                registerInstalled(it.metadata, it.directory)
        }
    }

    private fun registerInstalled(metadata: PluginManifestMetadata, directory: File) {
        val descriptor = metadata.descriptor
        val compatibilityFailure = try {
            metadata.requireCompatible(); null
        } catch (error: Exception) {
            error
        }
        runtime.register(
            PluginRuntimeController.Candidate(
                descriptor,
            "installed:${directory.absolutePath}",
            create = {
                // Revalidate immediately before class loading, even for dependency-driven activation.
                validateInstalledPlugin(pluginRoot, descriptor)
                classLoaderFactory.create(directory, descriptor).loadClass(descriptor.entryClass)
                    .getDeclaredConstructor().newInstance() as CosmicPlugin
            },
            context = {
                DefaultPluginContext(
                    descriptor, extensionRegistry,
                    serviceRegistry.copy()
                        .apply { register(AndroidPluginServices.PLUGIN_DIRECTORY, directory) },
                    AndroidPluginLogger(descriptor.id)
                )
            },
            failure = compatibilityFailure
        )
        )
    }
}

/** Diagnostic ids must use exactly the descriptor's ASCII grammar, including for Unicode names. */
internal fun invalidPluginDescriptor(directory: File): PluginDescriptor {
    val safeName =
        directory.name.map { if (it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it in "._-") it else '_' }
            .joinToString("")
    return PluginDescriptor(
        "org.cosmicide.invalid.$safeName", directory.name.ifBlank { "Invalid package" },
        "0.0.0", "unknown", source = "invalid"
    )
}
