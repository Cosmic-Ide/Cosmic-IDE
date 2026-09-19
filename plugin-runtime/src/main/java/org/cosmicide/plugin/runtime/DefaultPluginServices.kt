/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cosmicide.plugin.api.PluginEvents
import org.cosmicide.plugin.api.PluginSettings
import org.cosmicide.plugin.api.PluginStorage
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [PluginEvents].
 */
class DefaultPluginEvents : PluginEvents {
    private val flows = ConcurrentHashMap<Class<*>, MutableSharedFlow<Any>>()

    private fun getOrCreateFlow(eventClass: Class<*>): MutableSharedFlow<Any> {
        return flows.getOrPut(eventClass) {
            MutableSharedFlow(
                replay = 1,
                extraBufferCapacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }
    }

    override fun <T : Any> emit(eventClass: Class<T>, event: T) {
        getOrCreateFlow(eventClass).tryEmit(event)
    }

    override fun <T : Any> observe(eventClass: Class<T>): Flow<T> {
        @Suppress("UNCHECKED_CAST")
        return getOrCreateFlow(eventClass).asSharedFlow() as Flow<T>
    }
}

/**
 * Namespaced implementation of [PluginSettings] backed by [SharedPreferences].
 */
class DefaultPluginSettings(
    context: Context,
    pluginId: String
) : PluginSettings {
    private val prefs: SharedPreferences? = try {
        context.getSharedPreferences("plugin_settings_$pluginId", Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }

    private val stringFlows = ConcurrentHashMap<String, MutableStateFlow<String>>()
    private val booleanFlows = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    private val intFlows = ConcurrentHashMap<String, MutableStateFlow<Int>>()

    override fun getString(key: String, default: String): String =
        prefs?.getString(key, default) ?: default

    override fun putString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
        stringFlows[key]?.value = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs?.getBoolean(key, default) ?: default

    override fun putBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
        booleanFlows[key]?.value = value
    }

    override fun getInt(key: String, default: Int): Int =
        prefs?.getInt(key, default) ?: default

    override fun putInt(key: String, value: Int) {
        prefs?.edit()?.putInt(key, value)?.apply()
        intFlows[key]?.value = value
    }

    override fun observeString(key: String, default: String): Flow<String> {
        val flow = stringFlows.getOrPut(key) { MutableStateFlow(getString(key, default)) }
        return flow.asStateFlow()
    }

    override fun observeBoolean(key: String, default: Boolean): Flow<Boolean> {
        val flow = booleanFlows.getOrPut(key) { MutableStateFlow(getBoolean(key, default)) }
        return flow.asStateFlow()
    }

    override fun observeInt(key: String, default: Int): Flow<Int> {
        val flow = intFlows.getOrPut(key) { MutableStateFlow(getInt(key, default)) }
        return flow.asStateFlow()
    }
}

/**
 * Isolated implementation of [PluginStorage].
 */
class DefaultPluginStorage(
    context: Context,
    pluginId: String
) : PluginStorage {
    private val safeId = pluginId.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    override val dataDir: File = run {
        val root = try {
            context.filesDir
        } catch (_: Exception) {
            null
        } ?: File("/tmp")
        File(root, "plugin_data/$safeId").also { runCatching { it.mkdirs() } }
    }

    override val cacheDir: File = run {
        val root = try {
            context.cacheDir
        } catch (_: Exception) {
            null
        } ?: File("/tmp")
        File(root, "plugin_cache/$safeId").also { runCatching { it.mkdirs() } }
    }
}
