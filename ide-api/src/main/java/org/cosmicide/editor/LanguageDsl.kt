/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.editor

import org.cosmicide.plugin.api.Disposable
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.register

/**
 * Helper for contributing a TextMate syntax highlighting grammar for file extensions.
 */
fun PluginContext.registerTextMateGrammar(
    id: String,
    displayName: String,
    grammarLink: String,
    priority: Int = 350,
    vararg fileExtensions: String
): Disposable {
    require(id.isNotBlank()) { "Grammar id must not be blank" }
    require(displayName.isNotBlank()) { "Display name must not be blank" }
    require(grammarLink.isNotBlank()) { "Grammar link must not be blank" }
    require(fileExtensions.isNotEmpty()) { "At least one file extension must be provided" }

    val extensionsSet = fileExtensions.map { it.lowercase().removePrefix(".") }.toSet()

    val provider = object : EditorLanguageProvider {
        override val id: String = id
        override val displayName: String = displayName

        override fun supports(request: EditorLanguageRequest): Boolean {
            return request.file.extension.lowercase() in extensionsSet
        }

        override fun configure(request: EditorLanguageRequest): Boolean {
            return true
        }
    }

    return register(EditorExtensionPoints.LANGUAGE_PROVIDER, provider, priority)
}
