/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.plugin

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cosmicide.common.AppDispatchers
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.api.PluginHandle
import org.cosmicide.plugin.api.PluginLoadResult
import org.cosmicide.plugin.api.PluginSetupAction
import org.cosmicide.plugin.api.PluginState
import org.cosmicide.plugin.runtime.AndroidPluginManager
import org.cosmicide.plugin.runtime.PluginManifestReader
import org.cosmicide.util.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

data class PluginRepositoryEntry(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val shortDescription: String,
    val detailedDescription: String,
    val source: String,
    val downloadUrl: String,
    val sha256: String
)

data class PluginInstallResult(
    val plugin: PluginHandle,
    val firstInstall: Boolean,
    val setupActions: List<PluginSetupAction>
)

class PluginMarketplace(
    context: Context,
    private val manager: () -> AndroidPluginManager?
) {
    private val appContext = context.applicationContext

    suspend fun fetch(repositoryUrl: String): List<PluginRepositoryEntry> =
        withContext(AppDispatchers.IO) {
            val json = downloadText(repositoryUrl, MAX_INDEX_BYTES)
            parsePluginRepository(json)
        }

    suspend fun install(entry: PluginRepositoryEntry): PluginInstallResult =
        withContext(AppDispatchers.IO) {
            val pluginManager = checkNotNull(manager()) { "Plugin runtime is not initialized" }
            val pluginRoot = FileUtil.pluginDir.apply { mkdirs() }
            val token = UUID.randomUUID().toString()
            val archive = appContext.cacheDir.resolve("plugin-$token.zip")
            val staging = pluginRoot.resolve(".install-$token")
            val backup = pluginRoot.resolve(".backup-${entry.id}-$token")
            val target = pluginRoot.resolve(entry.id)
            val firstInstall = !target.exists()
            val existingHandle = pluginManager.plugins.firstOrNull {
                it.descriptor.id == entry.id
            }
            require(
                target.isDirectory ||
                        existingHandle == null ||
                        existingHandle.state == PluginState.FAILED
            ) {
                "Bundled plugin ${entry.id} cannot be replaced from the marketplace"
            }

            try {
                downloadFile(entry.downloadUrl, archive, MAX_PLUGIN_ARCHIVE_BYTES)
                val actualDigest = archive.sha256()
                check(actualDigest.equals(entry.sha256, ignoreCase = true)) {
                    "Plugin checksum mismatch"
                }

                check(staging.mkdirs()) { "Could not create plugin staging directory" }
                extractPluginArchive(archive, staging)
                val descriptor = PluginManifestReader.read(staging)
                    ?: error("Plugin package is missing ${PluginManifestReader.MANIFEST_FILE}")
                val artifacts = validatePackage(entry, descriptor, staging)
                makeArtifactsReadOnly(artifacts)

                installStagedPlugin(
                    manager = pluginManager,
                    descriptor = descriptor,
                    staging = staging,
                    target = target,
                    backup = backup
                )

                val handle = pluginManager.plugins.firstOrNull {
                    it.descriptor.id == descriptor.id
                } ?: error("Plugin was installed but did not become active")
                check(
                    handle.descriptor == descriptor &&
                            handle.state == PluginState.ACTIVE
                ) {
                    "Plugin was installed but did not become active"
                }

                PluginInstallResult(
                    plugin = handle,
                    firstInstall = firstInstall,
                    setupActions = handle.setupActions.takeIf { firstInstall }.orEmpty()
                )
            } finally {
                archive.delete()
                if (staging.exists()) staging.deleteRecursively()
            }
        }

    suspend fun uninstall(pluginId: String) = withContext(AppDispatchers.IO) {
        val pluginManager = checkNotNull(manager()) { "Plugin runtime is not initialized" }
        val target = FileUtil.pluginDir.resolve(pluginId)
        require(target.isDirectory) {
            "Bundled plugin $pluginId cannot be uninstalled"
        }

        val descriptor = PluginManifestReader.read(target)
            ?: error("Installed plugin $pluginId has no valid manifest")
        require(descriptor.id == pluginId) { "Installed plugin id does not match its directory" }

        val token = UUID.randomUUID().toString()
        val removing = FileUtil.pluginDir.resolve(".remove-$pluginId-$token")
        pluginManager.unload(pluginId)
        try {
            check(target.renameTo(removing)) { "Could not prepare $pluginId for removal" }
            check(removing.deleteRecursively()) { "Could not remove $pluginId" }
            pluginManager.forget(pluginId)
        } catch (error: Throwable) {
            if (removing.exists() && !target.exists()) {
                removing.renameTo(target)
            }
            if (target.isDirectory) {
                pluginManager.load(descriptor)
            }
            throw error
        }
    }

    private fun installStagedPlugin(
        manager: AndroidPluginManager,
        descriptor: PluginDescriptor,
        staging: File,
        target: File,
        backup: File
    ) {
        val replacing = target.isDirectory
        if (target.exists() && !replacing) {
            error("Plugin install target is not a directory")
        }

        if (replacing) {
            manager.unload(descriptor.id)
            check(target.renameTo(backup)) { "Could not preserve the installed plugin" }
        }

        try {
            check(staging.renameTo(target)) { "Could not activate the downloaded plugin package" }
            when (val result = manager.load(descriptor)) {
                is PluginLoadResult.Loaded -> Unit
                is PluginLoadResult.Failed -> {
                    error("Plugin activation failed: ${result.reason}")
                }
            }
            if (backup.exists()) backup.deleteRecursively()
        } catch (error: Throwable) {
            manager.unload(descriptor.id)
            if (target.exists()) target.deleteRecursively()
            if (backup.exists() && backup.renameTo(target)) {
                PluginManifestReader.read(target)?.let(manager::load)
            }
            throw error
        }
    }

    private fun validatePackage(
        entry: PluginRepositoryEntry,
        descriptor: PluginDescriptor,
        staging: File
    ): List<File> {
        require(descriptor.id == entry.id) {
            "Plugin id ${descriptor.id} does not match repository entry ${entry.id}"
        }
        require(descriptor.version == entry.version) {
            "Plugin version ${descriptor.version} does not match repository entry ${entry.version}"
        }

        val artifacts = if (descriptor.classPath.isNotEmpty()) {
            descriptor.classPath.map { path ->
                require(!File(path).isAbsolute) {
                    "Marketplace plugin artifacts must use package-relative paths"
                }
                staging.resolve(path).canonicalFile.also { artifact ->
                    require(artifact.toPath().startsWith(staging.canonicalFile.toPath())) {
                        "Plugin artifact is outside the package"
                    }
                }
            }
        } else {
            listOf("plugin.apk", "plugin.dex", "plugin.jar").map(staging::resolve)
        }
        val loadableArtifacts = artifacts.filter(File::isFile)
        require(loadableArtifacts.isNotEmpty()) { "Plugin package has no loadable artifact" }
        return loadableArtifacts
    }

    private fun downloadText(url: String, maxBytes: Long): String {
        val bytes = openDownload(url).use { connection ->
            connection.inputStream.use { input ->
                input.readBytesWithLimit(maxBytes)
            }
        }
        return bytes.toString(Charsets.UTF_8)
    }

    private fun downloadFile(url: String, target: File, maxBytes: Long) {
        openDownload(url).use { connection ->
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= maxBytes) { "Plugin download is too large" }
                        output.write(buffer, 0, count)
                    }
                }
            }
        }
    }

    private fun openDownload(url: String): DownloadConnection {
        val uri = requireHttpsUrl(url, "Plugin repositories and packages")
        val connection = uri.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Cosmic-IDE-Plugin-Marketplace")
        connection.connect()
        require(connection.url.protocol.equals("https", ignoreCase = true)) {
            "Plugin download redirected to an insecure URL"
        }
        check(connection.responseCode in 200..299) {
            "Download failed with HTTP ${connection.responseCode}"
        }
        return DownloadConnection(connection)
    }

    private class DownloadConnection(
        private val connection: HttpURLConnection
    ) : AutoCloseable {
        val inputStream
            get() = connection.inputStream

        override fun close() {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_INDEX_BYTES = 1L * 1024 * 1024
        const val MAX_PLUGIN_ARCHIVE_BYTES = 25L * 1024 * 1024
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
    }
}

internal fun makeArtifactsReadOnly(artifacts: List<File>) {
    artifacts.forEach { artifact ->
        check(artifact.setReadOnly() || !artifact.canWrite()) {
            "Could not make plugin artifact read-only: ${artifact.name}"
        }
        check(!artifact.canWrite()) {
            "Plugin artifact is still writable: ${artifact.name}"
        }
    }
}

internal fun parsePluginRepository(json: String): List<PluginRepositoryEntry> {
    val root = JsonParser.parseString(json)
    val entries = when {
        root.isJsonArray -> root.asJsonArray
        root.isJsonObject -> root.asJsonObject.getAsJsonArray("plugins")
        else -> null
    } ?: error("Plugin repository must be a JSON array or contain a plugins array")

    val plugins = entries.mapIndexed { index, element ->
        require(element.isJsonObject) { "Plugin entry $index must be an object" }
        element.asJsonObject.toRepositoryEntry(index)
    }
    require(plugins.distinctBy(PluginRepositoryEntry::id).size == plugins.size) {
        "Plugin repository contains duplicate ids"
    }
    return plugins
}

private fun JsonObject.toRepositoryEntry(index: Int): PluginRepositoryEntry {
    fun required(name: String): String {
        return get(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: error("Plugin entry $index is missing $name")
    }

    fun optional(name: String): String? {
        return get(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    val id = required("id")
    val downloadUrl = required("downloadUrl")
    val sha256 = required("sha256").lowercase()
    val source = optional("source").orEmpty()
    require(id.matches(Regex("[A-Za-z0-9_.-]+"))) { "Invalid plugin id: $id" }
    requireHttpsUrl(downloadUrl, "Plugin download URL")
    if (source.isNotEmpty()) requireHttpsUrl(source, "Plugin source URL")
    require(sha256.matches(Regex("[0-9a-f]{64}"))) {
        "Plugin $id must provide a SHA-256 checksum"
    }

    return PluginRepositoryEntry(
        id = id,
        name = required("name"),
        version = required("version"),
        author = optional("author").orEmpty(),
        shortDescription = optional("shortDescription")
            ?: optional("description").orEmpty(),
        detailedDescription = optional("detailedDescription")
            ?: optional("description")
            ?: optional("shortDescription").orEmpty(),
        source = source,
        downloadUrl = downloadUrl,
        sha256 = sha256
    )
}

private fun requireHttpsUrl(value: String, label: String): URI {
    val uri = runCatching { URI(value) }
        .getOrElse { throw IllegalArgumentException("$label is invalid", it) }
    require(
        uri.isAbsolute &&
                uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null
    ) {
        "$label must use an absolute HTTPS URL without user information"
    }
    return uri
}

internal fun extractPluginArchive(archive: File, destination: File) {
    val destinationPath = destination.canonicalFile.toPath()
    var entryCount = 0
    var extractedBytes = 0L

    ZipInputStream(archive.inputStream().buffered()).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            entryCount += 1
            check(entryCount <= 256) { "Plugin package contains too many files" }
            require(!entry.name.contains('\\')) { "Invalid plugin package path" }

            val output = destination.resolve(entry.name).canonicalFile
            require(output.toPath().startsWith(destinationPath)) {
                "Plugin package tries to write outside its directory"
            }

            if (entry.isDirectory) {
                check(output.mkdirs() || output.isDirectory) {
                    "Could not create plugin package directory"
                }
            } else {
                val parent = checkNotNull(output.parentFile) {
                    "Plugin package entry has no parent directory"
                }
                check(parent.mkdirs() || parent.isDirectory) {
                    "Could not create plugin package directory"
                }
                output.outputStream().use { stream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        extractedBytes += count
                        check(extractedBytes <= 50L * 1024 * 1024) {
                            "Plugin package expands beyond the allowed size"
                        }
                        stream.write(buffer, 0, count)
                    }
                }
            }
            zip.closeEntry()
        }
    }
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun java.io.InputStream.readBytesWithLimit(maxBytes: Long): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        check(total <= maxBytes) { "Plugin repository index is too large" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
