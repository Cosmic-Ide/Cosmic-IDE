/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.util

object PreferenceKeys {

    // Appearance
    const val APP_THEME = "app_theme"
    const val APP_ACCENT = "app_accent"

    // Compiler
    const val COMPILER_USE_FJFS = "use_fast_jar_file_system"
    const val COMPILER_USE_K2 = "use_k2"
    const val COMPILER_USE_SSVM = "use_ssvm"
    const val COMPILER_JAVA_VERSIONS = "java_versions"
    const val COMPILER_JAVAC_FLAGS = "javac_flags"
    const val COMPILER_KOTLIN_VERSION = "kotlin_version"

    // Editor
    const val EDITOR_FONT_SIZE = "font_size"
    const val EDITOR_TAB_SIZE = "tab_size"
    const val EDITOR_USE_SPACES = "use_spaces"
    const val EDITOR_LIGATURES_ENABLE = "ligatures_enable"
    const val EDITOR_WORDWRAP_ENABLE = "wordwrap_enable"
    const val EDITOR_SCROLLBAR_SHOW = "scrollbar_show"
    const val EDITOR_HW_ENABLE = "hardware_acceleration_enable"
    const val EDITOR_NON_PRINTABLE_SYMBOLS_SHOW = "non_printable_symbols_show"
    const val EDITOR_LINE_NUMBERS_SHOW = "line_numbers_show"
    const val EDITOR_DOUBLE_CLICK_CLOSE = "double_click_close"
    const val EDITOR_EXP_JAVA_COMPLETION = "experimental_java_completion"
    const val KOTLIN_REALTIME_ERRORS = "kotlin_realtime_errors"
    const val EDITOR_FONT = "editor_font"
    const val BRACKET_PAIR_AUTOCOMPLETE = "bracket_pair_autocomplete"
    const val QUICK_DELETE = "quick_delete"
    const val STICKY_SCROLL = "sticky_scroll"

    // Formatter
    const val FORMATTER_KTFMT_STYLE = "ktfmt_style"
    const val FORMATTER_GJF_STYLE = "google_java_formatter_style"
    const val FORMATTER_GJF_OPTIONS = "google_java_formatter_options"

    // Git
    const val GIT_USERNAME = "git_username"
    const val GIT_EMAIL = "git_email"
    const val GIT_API_KEY = "git_api_key"

    // Plugins
    const val AVAILABLE_PLUGINS = "available_plugins"
    const val INSTALLED_PLUGINS = "installed_plugins"
    const val PLUGIN_REPOSITORY = "plugin_repository"
    const val PLUGIN_SETTINGS = "plugin_settings"

}
