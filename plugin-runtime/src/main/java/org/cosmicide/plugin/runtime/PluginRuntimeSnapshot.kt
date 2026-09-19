/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.PluginState

/** Immutable diagnostic values; never retains plugin instances, contexts, or mutable metadata. */
data class PluginRuntimeSnapshot(
    val id: String,
    val version: String,
    val state: PluginState,
    val errorMessage: String? = null,
    val failureKind: PluginFailureKind? = null,
    val transition: PluginRuntimeTransition? = null
)

enum class PluginRuntimeTransition { ACTIVATING, DEACTIVATING }
