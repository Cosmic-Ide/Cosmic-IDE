/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.PluginDependency
import org.cosmicide.plugin.api.PluginDescriptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PluginManifestReader {
    const val MANIFEST_FILE = "plugin.json"

    fun read(pluginDir: File): PluginDescriptor? {
        val manifest = pluginDir.resolve(MANIFEST_FILE)
        if (!manifest.isFile) return null
        return readDescriptor(manifest.readText())
    }

    fun readDescriptor(jsonText: String): PluginDescriptor {
        val json = JSONObject(jsonText)
        return PluginDescriptor(
            id = json.getString("id"),
            name = json.optString("name", json.getString("id")),
            version = json.optString("version", "0.0.0"),
            entryClass = json.getString("entryClass"),
            description = json.optString("description", ""),
            author = json.optString("author", ""),
            source = json.optString("source", ""),
            classPath = json.optStringArray("classPath") +
                    json.optStringArray("classpath") +
                    listOfNotNull(json.optString("artifact", "").takeIf { it.isNotBlank() }),
            dependencies = json.optDependencyArray("dependencies"),
            capabilities = json.optStringArray("capabilities").toSet(),
            enabledByDefault = json.optBoolean("enabledByDefault", true)
        )
    }

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
