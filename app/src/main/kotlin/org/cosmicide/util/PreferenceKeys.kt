/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.util

object PreferenceKeys {

    const val COMPILER_CURRENT_JDK = "current_jdk"

    // Editor
    const val EDITOR_FONT_SIZE = "font_size"
    const val EDITOR_TAB_SIZE = "tab_size"
    const val EDITOR_USE_SPACES = "use_spaces"
    const val EDITOR_LIGATURES_ENABLE = "font_ligatures"
    const val EDITOR_WORDWRAP_ENABLE = "word_wrap"
    const val EDITOR_SCROLLBAR_SHOW = "scrollbar"
    const val EDITOR_HW_ENABLE = "hardware_acceleration"
    const val EDITOR_NON_PRINTABLE_SYMBOLS_SHOW = "non_printable_characters"
    const val EDITOR_LINE_NUMBERS_SHOW = "line_numbers"
    const val EDITOR_FONT = "editor_font"
    const val BRACKET_PAIR_AUTOCOMPLETE = "bracket_pair_autocomplete"
    const val QUICK_DELETE = "quick_delete"
    const val STICKY_SCROLL = "sticky_scroll"

    const val PLUGIN_REPOSITORY = "plugin_repository"
    const val CUSTOM_LSP_CONFIGURATIONS = "custom_lsp_configurations"
    const val CUSTOM_PROJECT_TYPES = "custom_project_types"

    const val EXTENSION_ENABLED_PREFIX = "extension_enabled."

}
