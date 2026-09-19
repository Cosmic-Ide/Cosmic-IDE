/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.api

import kotlinx.coroutines.flow.Flow

/**
 * Namespaced key-value settings interface for plugins with Flow observation.
 */
interface PluginSettings {
    fun getString(key: String, default: String = ""): String
    fun putString(key: String, value: String)

    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)

    fun getInt(key: String, default: Int = 0): Int
    fun putInt(key: String, value: Int)

    fun observeString(key: String, default: String = ""): Flow<String>
    fun observeBoolean(key: String, default: Boolean = false): Flow<Boolean>
    fun observeInt(key: String, default: Int = 0): Flow<Int>

    companion object {
        @JvmField
        val KEY = ServiceKey("org.cosmicide.plugin.settings", PluginSettings::class.java)
    }
}
