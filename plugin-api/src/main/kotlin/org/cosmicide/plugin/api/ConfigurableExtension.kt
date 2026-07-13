/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.plugin.api

/** Stable identity and user-facing metadata for an extension contribution. */
interface ConfigurableExtension {
    val id: String

    val displayName: String
        get() = id

    val description: String
        get() = ""

    val enabledByDefault: Boolean
        get() = true

    val canDisable: Boolean
        get() = true
}
