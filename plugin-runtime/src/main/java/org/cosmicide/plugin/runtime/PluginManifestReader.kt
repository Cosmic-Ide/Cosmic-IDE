/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.HostApiRange
import org.cosmicide.plugin.api.PluginApiRequirements
import org.cosmicide.plugin.api.PluginCompatibility
import org.cosmicide.plugin.api.PluginDependency
import org.cosmicide.plugin.api.PluginDescriptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Parsed manifest metadata. */
data class PluginManifestMetadata(
    val descriptor: PluginDescriptor,
    val schemaVersion: Int?,
    val requirements: PluginApiRequirements = PluginApiRequirements()
) {
    fun requireCompatible() = PluginCompatibility.requireCompatible(requirements)
}

object PluginManifestReader {
    const val MANIFEST_FILE = "plugin.json"
    const val SUPPORTED_SCHEMA_VERSION = 1
    const val MAX_MANIFEST_BYTES = 64 * 1024
    const val MAX_CLASSPATH_ENTRIES = 64
    const val MAX_DEPENDENCIES = 128
    const val MAX_CAPABILITIES = 128

    fun read(pluginDir: File): PluginDescriptor? = readMetadata(pluginDir)?.descriptor

    fun readMetadata(pluginDir: File): PluginManifestMetadata? {
        val manifest = pluginDir.resolve(MANIFEST_FILE)
        if (!manifest.isFile) return null
        require(manifest.length() <= MAX_MANIFEST_BYTES) { "Manifest exceeds $MAX_MANIFEST_BYTES bytes" }
        // Fixed cap plus one sentinel byte also bounds files that grow after the length check.
        val bytes = ByteArray(MAX_MANIFEST_BYTES + 1)
        var size = 0
        manifest.inputStream().use { input ->
            while (size < bytes.size) {
                val count = input.read(bytes, size, bytes.size - size)
                if (count == -1) break
                size += count
            }
        }
        require(size <= MAX_MANIFEST_BYTES) { "Manifest exceeds $MAX_MANIFEST_BYTES bytes" }
        return readMetadata(String(bytes, 0, size, Charsets.UTF_8))
    }

    fun readDescriptor(jsonText: String): PluginDescriptor = readMetadata(jsonText).descriptor

    fun readMetadata(jsonText: String): PluginManifestMetadata {
        // Check character count first so even the UTF-8 conversion has bounded allocation.
        require(jsonText.length <= MAX_MANIFEST_BYTES &&
                jsonText.toByteArray(Charsets.UTF_8).size <= MAX_MANIFEST_BYTES) {
            "Manifest exceeds $MAX_MANIFEST_BYTES bytes"
        }
        val json = JSONObject(jsonText)
        val schemaVersion = if (json.has("schemaVersion")) {
            val value = json.get("schemaVersion")
            require((value is Int || value is Long) && (value as Number).toLong() == SUPPORTED_SCHEMA_VERSION.toLong()) {
                "schemaVersion must be the supported integer $SUPPORTED_SCHEMA_VERSION"
            }
            SUPPORTED_SCHEMA_VERSION
        } else null
        // Unversioned manifests retain their old treatment of unknown fields.
        val requirements = if (schemaVersion == SUPPORTED_SCHEMA_VERSION && json.has("compatibility")) {
            val compatibility = json.get("compatibility")
            require(compatibility is JSONObject) { "compatibility must be an object" }
            PluginApiRequirements(
                compatibility.optApiRange("pluginApi"),
                compatibility.optApiRange("ideApi")
            )
        } else PluginApiRequirements()
        // Incompatible declared API ranges fail during parsing so every entry point, including
        // marketplace staging reads, rejects the package before any code execution.
        PluginCompatibility.requireCompatible(requirements)
        // Count raw slots before filtering/deduplication, and combine legacy classpath aliases.
        val artifact = json.optString("artifact", "").takeIf { it.isNotBlank() }
        require(json.arraySize("classPath") + json.arraySize("classpath") +
                (if (artifact == null) 0 else 1) <= MAX_CLASSPATH_ENTRIES) {
            "Manifest exceeds $MAX_CLASSPATH_ENTRIES classpath entries"
        }
        require(json.arraySize("dependencies") <= MAX_DEPENDENCIES) {
            "Manifest exceeds $MAX_DEPENDENCIES dependencies"
        }
        require(json.arraySize("capabilities") <= MAX_CAPABILITIES) {
            "Manifest exceeds $MAX_CAPABILITIES capabilities"
        }
        val descriptor = PluginDescriptor(
            id = json.getString("id"),
            name = json.optString("name", json.getString("id")),
            version = json.optString("version", "0.0.0"),
            entryClass = json.getString("entryClass"),
            description = json.optString("description", ""),
            author = json.optString("author", ""),
            source = json.optString("source", ""),
            classPath = json.optStringArray("classPath") +
                    json.optStringArray("classpath") +
                    listOfNotNull(artifact),
            dependencies = json.optDependencyArray("dependencies"),
            capabilities = json.optStringArray("capabilities").toSet(),
            enabledByDefault = json.optBoolean("enabledByDefault", true)
        )
        return PluginManifestMetadata(descriptor, schemaVersion, requirements)
    }

    private fun JSONObject.optApiRange(name: String): HostApiRange? {
        if (!has(name)) return null
        val range = get(name)
        require(range is JSONObject) { "$name must be an API range object" }
        val min = range.opt("minInclusive")
        val max = range.opt("maxExclusive")
        require(min is String && max is String) {
            "$name requires string minInclusive and maxExclusive bounds"
        }
        return HostApiRange(min, max)
    }

    private fun JSONObject.arraySize(name: String): Int = optJSONArray(name)?.length() ?: 0

    private fun JSONObject.optStringArray(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return array.toStringList()
    }

    private fun JSONArray.toStringList(): List<String> {
        val values = mutableListOf<String>()
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(values::add)
        }
        return values
    }

    private fun JSONObject.optDependencyArray(name: String): List<PluginDependency> {
        val array = optJSONArray(name) ?: return emptyList()
        val dependencies = mutableListOf<PluginDependency>()
        for (index in 0 until array.length()) {
            when (val value = array.get(index)) {
                is String -> dependencies += PluginDependency(value)
                is JSONObject -> dependencies += PluginDependency(
                    id = value.getString("id"),
                    minVersion = value.optString("minVersion").takeIf { it.isNotBlank() },
                    optional = value.optBoolean("optional", false)
                )
            }
        }
        return dependencies
    }

}
