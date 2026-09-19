/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.common

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

/**
 * A utility object to access shared preferences easily.
 */
object Prefs {

    private lateinit var prefs: SharedPreferences

    /**
     * Initializes shared preferences.
     * @param context The context of the application.
     */
    fun init(context: Context) {
        prefs =
            context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        migrateEditorPreferences()
    }

    val appTheme: String
        get() = prefs.getString("app_theme", "auto") ?: "auto"

    val currentJDK: String
        get() = prefs.getString("current_jdk", "system") ?: "system"

    val stickyScroll: Boolean
        get() = prefs.getBoolean("sticky_scroll", false)

    val useLigatures: Boolean
        get() = prefs.getBoolean("font_ligatures", true)

    val wordWrap: Boolean
        get() = prefs.getBoolean("word_wrap", false)

    val scrollbarEnabled: Boolean
        get() = prefs.getBoolean("scrollbar", true)

    val minimap: Boolean
        get() = prefs.getBoolean("minimap", false)

    val hardwareAcceleration: Boolean
        get() = prefs.getBoolean("hardware_acceleration", true)

    val nonPrintableCharacters: Boolean
        get() = prefs.getBoolean("non_printable_characters", false)

    val lineNumbers: Boolean
        get() = prefs.getBoolean("line_numbers", true)

    val useSpaces: Boolean
        get() = prefs.getBoolean("use_spaces", false)

    val tabSize: Int
        get() = when (val value = prefs.all["tab_size"]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }?.coerceIn(1, 16) ?: 4

    val bracketPairAutocomplete: Boolean
        get() = prefs.getBoolean("bracket_pair_autocomplete", true)

    val quickDelete: Boolean
        get() = prefs.getBoolean("quick_delete", false)

    val analyticsEnabled: Boolean
        get() = prefs.getBoolean("analytics_preference", true)

    val editorFont: String
        get() = prefs.getString("editor_font", "") ?: ""

    val editorTheme: String
        get() = prefs.getString("editor_theme", "auto") ?: "auto"

    val repositories: String
        get() = prefs.getString("repos", "") ?: """
            Maven Central: https://repo1.maven.org/maven2
            Google Maven: https://maven.google.com
            Jitpack: https://jitpack.io
            Sonatype Snapshots: https://s01.oss.sonatype.org/content/repositories/snapshots
            JCenter: https://jcenter.bintray.com
        """.trimIndent()

    val pluginRepository: String
        get() = prefs.getString(
            "plugin_repository",
            "https://raw.githubusercontent.com/Cosmic-IDE/plugins-repo/main/plugins.json",
        ) ?: "https://raw.githubusercontent.com/Cosmic-IDE/plugins-repo/main/plugins.json"

    val editorFontSize: Float
        get() {
            val value = when (val stored = prefs.all["font_size"]) {
                is Number -> stored.toFloat()
                is String -> stored.trim().toFloatOrNull()
                else -> null
            }
            return value?.takeIf(Float::isFinite)?.coerceIn(4f, 32f) ?: 8f
        }

    val clientName: String
        get() = prefs.getString("client_name", null)?.replace(" ", "") ?: Build.ID

    val backupToFs: Boolean
        get() = prefs.getBoolean("backup_to_fs", false)

    private fun migrateEditorPreferences() {
        val aliases = mapOf(
            "ligatures_enable" to "font_ligatures",
            "wordwrap_enable" to "word_wrap",
            "scrollbar_show" to "scrollbar",
            "hardware_acceleration_enable" to "hardware_acceleration",
            "non_printable_symbols_show" to "non_printable_characters",
            "line_numbers_show" to "line_numbers"
        )
        val values = prefs.all
        prefs.edit().apply {
            aliases.forEach { (legacyKey, currentKey) ->
                if (!values.containsKey(currentKey)) {
                    (values[legacyKey] as? Boolean)?.let { putBoolean(currentKey, it) }
                }
                remove(legacyKey)
            }

            (values["font_size"] as? Number)?.let {
                putString("font_size", it.toFloat().toString())
            }
            (values["tab_size"] as? Number)?.let {
                putInt("tab_size", it.toInt())
            }
            remove("double_click_close")
            remove("disable_symbols_view")
        }.apply()
    }
}

internal fun parseIntPreference(value: String?, default: Int): Int =
    value?.trim()?.toIntOrNull() ?: default

internal fun parseBoundedFloatPreference(
    value: String?,
    default: Float,
    minimum: Float,
    maximum: Float
): Float {
    require(minimum <= maximum) { "Minimum must not exceed maximum" }
    val parsed = value?.trim()?.toFloatOrNull()?.takeIf(Float::isFinite) ?: return default
    return parsed.coerceIn(minimum, maximum)
}
