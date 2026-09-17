/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.PluginDescriptor
import java.io.File

sealed interface PluginDiscoveryResult {
    val directory: File
    data class Valid(val metadata: PluginManifestMetadata, override val directory: File) : PluginDiscoveryResult
    data class Invalid(override val directory: File, val reason: String, val cause: Exception) : PluginDiscoveryResult
}

/** Canonically confined, visible direct child. Used before installed package reads/mutations. */
fun installedPluginDirectory(root: File, id: String): File {
    require(id.matches(Regex("[A-Za-z0-9_.-]+")) && !id.startsWith(".")) {
        "Invalid installed plugin id: $id"
    }
    val canonicalRoot = root.canonicalFile
    val target = root.resolve(id).canonicalFile
    require(target.parentFile == canonicalRoot && target.name == id) {
        "Plugin installation must be a confined directory matching its id: $id"
    }
    return target
}

/** Re-read on every load, including the legacy descriptor-only facade; never trust lost metadata. */
internal fun validateInstalledPlugin(root: File, descriptor: PluginDescriptor): PluginManifestMetadata {
    val directory = installedPluginDirectory(root, descriptor.id)
    val metadata = PluginManifestReader.readMetadata(directory)
        ?: error("Plugin package is missing ${PluginManifestReader.MANIFEST_FILE}")
    require(metadata.descriptor == descriptor) { "Installed plugin manifest does not match requested descriptor" }
    metadata.requireCompatible()
    return metadata
}

/** Metadata only. Invalid manifests do not prevent later packages from being discovered. */
object PluginDiscovery {
    fun discover(pluginRoot: File): List<PluginDiscoveryResult> = pluginRoot
        .listFiles { it.isDirectory && !it.name.startsWith(".") }
        ?.sortedBy { it.name }.orEmpty().map { directory ->
            try {
                installedPluginDirectory(pluginRoot, directory.name)
                val metadata = PluginManifestReader.readMetadata(directory)
                    ?: error("Missing ${PluginManifestReader.MANIFEST_FILE}")
                require(metadata.descriptor.id == directory.name) { "Installed plugin id does not match its directory" }
                // Keep incompatible, parseable metadata so the manager can return a Failed handle.
                PluginDiscoveryResult.Valid(metadata, directory)
            } catch (error: Exception) {
                PluginDiscoveryResult.Invalid(directory, error.message ?: "Invalid plugin manifest", error)
            }
        }
}
