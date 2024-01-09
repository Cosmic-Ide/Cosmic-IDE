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
import androidx.preference.PreferenceManager

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
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    val isInitialized: Boolean
        get() = Prefs::prefs.isInitialized

    val appTheme: String
        get() = prefs.getString("app_theme", "auto") ?: "auto"

    val appAccent: String
        get() = prefs.getString("app_accent", "default") ?: "default"

    val useFastJarFs: Boolean
        get() = prefs.getBoolean("use_fastjarfs", true)

    val stickyScroll: Boolean
        get() = prefs.getBoolean("sticky_scroll", false)

    val useLigatures: Boolean
        get() = prefs.getBoolean("font_ligatures", true)

    val wordWrap: Boolean
        get() = prefs.getBoolean("word_wrap", false)

    val scrollbarEnabled: Boolean
        get() = prefs.getBoolean("scrollbar", true)

    val hardwareAcceleration: Boolean
        get() = prefs.getBoolean("hardware_acceleration", true)

    val nonPrintableCharacters: Boolean
        get() = prefs.getBoolean("non_printable_characters", false)

    val ktfmtStyle: String
        get() = prefs.getString("ktfmt_style", "google") ?: "google"

    val googleJavaFormatOptions: Set<String>?
        get() = prefs.getStringSet("google_java_formatter_options", setOf())

    val googleJavaFormatStyle: String
        get() = prefs.getString("google_java_formatter_style", "aosp") ?: "aosp"
    val lineNumbers: Boolean
        get() = prefs.getBoolean("line_numbers", true)

    val useSpaces: Boolean
        get() = prefs.getBoolean("use_spaces", false)

    val tabSize: Int
        get() = prefs.getInt("tab_size", 4)

    val bracketPairAutocomplete: Boolean
        get() = prefs.getBoolean("bracket_pair_autocomplete", true)

    val quickDelete: Boolean
        get() = prefs.getBoolean("quick_delete", false)

    val javacFlags: String
        get() = prefs.getString("javac_flags", "") ?: ""

    val compilerJavaVersion: Int
        get() = Integer.parseInt(prefs.getString("java_version", "17") ?: "17")

    val kotlinVersion: String
        get() = prefs.getString("kotlin_version", "2.1") ?: "2.1"

    val analyticsEnabled: Boolean
        get() = prefs.getBoolean("analytics_preference", true)

    val doubleClickClose: Boolean
        get() = prefs.getBoolean("double_click_close", false)

    val disableSymbolsView: Boolean
        get() = prefs.getBoolean("disable_symbols_view", false)

    val experimentalJavaCompletion: Boolean
        get() = prefs.getBoolean("experimental_java_completion", false)

    val gitUsername: String
        get() = prefs.getString("git_username", "") ?: ""

    val gitEmail: String
        get() = prefs.getString("git_email", "") ?: ""

    val gitApiKey: String
        get() = prefs.getString("git_api_key", "") ?: ""

    val kotlinRealtimeErrors: Boolean
        get() = prefs.getBoolean("kotlin_realtime_errors", false)

    val experimentsEnabled: Boolean
        get() = prefs.getBoolean("experiments_enabled", false)


    val editorFont: String
        get() = prefs.getString("editor_font", "") ?: ""

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
            "https://raw.githubusercontent.com/Cosmic-IDE/plugins-repo/main/plugins.json"
        ) ?: "https://raw.githubusercontent.com/Cosmic-IDE/plugins-repo/main/plugins.json"

    val editorFontSize: Float
        get() = runCatching {
            prefs.getString("font_size", "14")?.toFloatOrNull()?.coerceIn(1f, 32f) ?: 14f
        }.getOrElse { 16f }

    val geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""

    val temperature: Float
        get() = runCatching {
            prefs.getString("temperature", "0.9")?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.9f
        }.getOrElse { 0.9f }

    val topP: Float
        get() = runCatching {
            prefs.getString("top_p", "1.0")?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1.0f
        }.getOrElse { 1.0f }

    val topK: Int
        get() = prefs.getInt("top_k", 1).coerceIn(1, 60)

    val maxTokens: Int
        get() = prefs.getInt("max_tokens", 1024).coerceIn(60, 2048)

    val clientName: String
        get() = prefs.getString("client_name", null)?.replace(" ", "") ?: Build.ID
}
