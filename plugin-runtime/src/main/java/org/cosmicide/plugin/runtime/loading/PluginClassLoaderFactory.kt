/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime.loading

import android.content.Context
import dalvik.system.DexClassLoader
import org.cosmicide.plugin.api.PluginDescriptor
import java.io.File

class PluginClassLoaderFactory(
    private val context: Context
) {

    fun create(pluginDir: File, descriptor: PluginDescriptor): ClassLoader {
        val artifacts = resolveArtifacts(pluginDir, descriptor)
        require(artifacts.isNotEmpty()) {
            "Plugin ${descriptor.id} has no loadable artifact. Expected classPath in plugin.json or plugin.apk/plugin.dex/plugin.jar."
        }

        artifacts.forEach {
            SharedPluginClassLoader.loadDex(it)
        }

        val optimizedDir = File(context.codeCacheDir, "plugins/${descriptor.id}").apply {
            mkdirs()
        }
        val libraryPath = pluginDir.resolve("lib").takeIf { it.isDirectory }?.absolutePath
        val dexPath = artifacts.joinToString(File.pathSeparator) { it.absolutePath }

        return DexClassLoader(
            dexPath,
            optimizedDir.absolutePath,
            libraryPath,
            context.classLoader
        )
    }

    fun resolveArtifacts(pluginDir: File, descriptor: PluginDescriptor): List<File> {
        return resolvePluginArtifacts(pluginDir, descriptor)
    }
}

internal fun resolvePluginArtifacts(
    pluginDir: File,
    descriptor: PluginDescriptor
): List<File> {
    val declared = descriptor.classPath.map { path ->
        File(path).takeIf { it.isAbsolute } ?: pluginDir.resolve(path)
    }
    if (declared.isNotEmpty()) return declared.filter { it.isFile }

    val knownNames = listOf("plugin.apk", "plugin.dex", "plugin.jar")
        .map { pluginDir.resolve(it) }
        .filter { it.isFile }
    if (knownNames.isNotEmpty()) return knownNames

    return pluginDir
        .listFiles { file ->
            file.isFile && file.extension in setOf("apk", "dex", "jar")
        }
        ?.sortedBy { it.name }
        .orEmpty()
}
