/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime.loading

import android.content.Context
import android.os.Build
import dalvik.system.DexClassLoader
import org.cosmicide.plugin.api.PluginDescriptor
import java.io.File

/**
 * Creates an isolated class loader for each installed plugin.
 *
 * Plugin API and application classes are resolved from [appClassLoader], while
 * implementation classes and bundled dependencies are resolved from the
 * plugin's own APK, DEX, or JAR artifacts.
 */
class PluginClassLoaderFactory(
    context: Context
) {

    private val appContext = context.applicationContext
    private val appClassLoader: ClassLoader = requireNotNull(appContext.classLoader) {
        "Application class loader is unavailable"
    }

    fun create(
        pluginDir: File, descriptor: PluginDescriptor
    ): ClassLoader {
        val canonicalPluginDir = pluginDir.canonicalFile

        require(canonicalPluginDir.isDirectory) {
            "Plugin directory does not exist: ${canonicalPluginDir.absolutePath}"
        }

        val artifacts = resolvePluginArtifacts(
            pluginDir = canonicalPluginDir, descriptor = descriptor
        )

        require(artifacts.isNotEmpty()) {
            "Plugin ${descriptor.id} has no loadable artifact. " + "Expected classPath entries or plugin.apk/plugin.dex/plugin.jar."
        }

        artifacts.forEach { artifact ->
            require(artifact.isFile) {
                "Plugin artifact does not exist: ${artifact.absolutePath}"
            }

            require(artifact.canRead()) {
                "Plugin artifact is not readable: ${artifact.absolutePath}"
            }
        }

        val optimizedDirectory = createOptimizedDirectory(descriptor)
        val dexPath = artifacts.joinToString(File.pathSeparator) {
            it.absolutePath
        }

        return DexClassLoader(
            dexPath,
            optimizedDirectory.absolutePath,
            resolveNativeLibraryPath(canonicalPluginDir),
            appClassLoader
        )
    }

    private fun createOptimizedDirectory(
        descriptor: PluginDescriptor
    ): File {
        val safePluginId = descriptor.id.replace(
            INVALID_CACHE_NAME_CHARACTERS, "_"
        )

        return File(
            appContext.codeCacheDir, "plugins/$safePluginId"
        ).apply {
            require(isDirectory || mkdirs()) {
                "Could not create plugin code-cache directory: $absolutePath"
            }
        }
    }

    /**
     * Supports both:
     *
     * plugin/lib/(*).so
     * plugin/lib/arm64-v8a/(*).so
     */
    private fun resolveNativeLibraryPath(
        pluginDir: File
    ): String? {
        val libraryRoot = pluginDir.resolve("lib").takeIf(File::isDirectory) ?: return null

        val directories = buildList {
            Build.SUPPORTED_ABIS.forEach { abi ->
                libraryRoot.resolve(abi).takeIf(File::isDirectory)?.let(::add)
            }

            add(libraryRoot)
        }.distinctBy {
            it.absolutePath
        }

        return directories.takeIf(List<File>::isNotEmpty)?.joinToString(File.pathSeparator) {
            it.absolutePath
        }
    }

    private companion object {
        val INVALID_CACHE_NAME_CHARACTERS = Regex("[^A-Za-z0-9._-]")
    }
}

internal fun resolvePluginArtifacts(
    pluginDir: File, descriptor: PluginDescriptor
): List<File> {
    val canonicalPluginDir = pluginDir.canonicalFile

    if (descriptor.classPath.isNotEmpty()) {
        return descriptor.classPath.mapNotNull { declaredPath ->
            require(declaredPath.isNotBlank()) {
                "Plugin ${descriptor.id} contains an empty classPath entry"
            }

            val relativeFile = File(declaredPath)

            require(!relativeFile.isAbsolute) {
                "Plugin ${descriptor.id} classPath entries must be relative: " + declaredPath
            }

            val artifact = canonicalPluginDir.resolve(declaredPath).let {
                if (it.exists()) it.canonicalFile else it
            }

            if (!artifact.exists()) {
                return@mapNotNull null
            }

            require(artifact.isInside(canonicalPluginDir)) {
                "Plugin ${descriptor.id} classPath escapes its installation " + "directory: $declaredPath"
            }

            require(artifact.isFile) {
                "Declared plugin artifact does not exist or is not a file: " + artifact.absolutePath
            }

            require(artifact.hasSupportedExtension()) {
                "Unsupported plugin artifact type: ${artifact.name}"
            }

            artifact
        }.distinctBy {
            it.absolutePath
        }
    }

    val knownArtifacts =
        KNOWN_ARTIFACT_NAMES.map { name -> canonicalPluginDir.resolve(name) }.filter(File::isFile)

    if (knownArtifacts.isNotEmpty()) {
        return knownArtifacts
    }

    return canonicalPluginDir.listFiles { file ->
        file.isFile && file.hasSupportedExtension()
    }?.sortedBy(File::getName).orEmpty()
}

private fun File.hasSupportedExtension(): Boolean {
    return extension.lowercase() in SUPPORTED_ARTIFACT_EXTENSIONS
}

private fun File.isInside(parent: File): Boolean {
    val parentPath = parent.canonicalFile.toPath()
    val childPath = canonicalFile.toPath()

    return childPath.startsWith(parentPath) && childPath != parentPath
}

private val KNOWN_ARTIFACT_NAMES = listOf(
    "plugin.apk", "plugin.dex", "plugin.jar"
)

private val SUPPORTED_ARTIFACT_EXTENSIONS = setOf(
    "apk", "dex", "jar"
)