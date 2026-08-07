/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor

import org.cosmicide.plugin.api.ExtensionPoint

object EditorExtensionPoints {
    @JvmField
    val LANGUAGE_PROVIDER = ExtensionPoint(
        "org.cosmicide.editor.languageProvider",
        EditorLanguageProvider::class.java
    )

    @JvmField
    val LSP_SERVER_PROVIDER = ExtensionPoint(
        "org.cosmicide.editor.lspServerProvider",
        LspServerProvider::class.java
    )

    @JvmField
    val FORMATTER_PROVIDER = ExtensionPoint(
        "org.cosmicide.editor.formatterProvider",
        EditorFormatterProvider::class.java
    )

    @JvmField
    val PREVIEW_PROVIDER = ExtensionPoint(
        "org.cosmicide.editor.previewProvider",
        EditorPreviewProvider::class.java
    )

    @JvmField
    val THEME_PROVIDER = ExtensionPoint(
        "org.cosmicide.editor.themeProvider",
        EditorThemeProvider::class.java
    )
}
