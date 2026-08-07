/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor

import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.cosmicide.plugin.api.ConfigurableExtension

/**
 * Contributes a TextMate color theme to the code editor.
 *
 * The returned [ThemeModel] must carry a stable, globally unique name so the app can select
 * the theme and replace it across reloads. The name passed to the two-argument constructor
 * `ThemeModel(source, name)` should match the theme's internal `name` field so repeated
 * registration deduplicates in the theme registry.
 */
interface EditorThemeProvider : ConfigurableExtension {
    /** Higher values take precedence when several themes match the active mode. */
    val priority: Int
        get() = 0

    /** Whether this theme is designed for dark mode. */
    val isDark: Boolean
        get() = false

    /** Builds the TextMate theme to register in the editor's theme registry. */
    fun createTheme(): ThemeModel
}
