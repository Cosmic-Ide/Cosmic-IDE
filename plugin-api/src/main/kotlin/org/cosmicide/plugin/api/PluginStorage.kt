/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.api

import java.io.File

/**
 * Provides isolated data and cache directories for a plugin.
 */
interface PluginStorage {
    val dataDir: File
    val cacheDir: File

    companion object {
        @JvmField
        val KEY = ServiceKey("org.cosmicide.plugin.storage", PluginStorage::class.java)
    }
}
