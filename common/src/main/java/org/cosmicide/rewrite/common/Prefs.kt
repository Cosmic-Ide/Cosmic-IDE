/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.common

import android.content.Context
import android.content.SharedPreferences
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

    val appTheme: String
        get() = prefs.getString("app_theme", "auto") ?: "auto"

    val useFastJarFs: Boolean
        get() = prefs.getBoolean("use_fastjarfs", false)

    val useK2: Boolean
        get() = prefs.getBoolean("use_k2", false)

    val useSpaces: Boolean
        get() = prefs.getBoolean("use_spaces", false)

    val tabSize: Int
        get() = prefs.getInt("tab_size", 4)

    val compilerJavaVersion: Int
        get() = Integer.parseInt(prefs.getString("java_version", "17"))

    val useSSVM: Boolean
        get() = prefs.getBoolean("use_ssvm", false)

    val editorFontSize: Float
        get() = runCatching {
            prefs.getString("font_size", "14")?.toFloatOrNull()?.coerceIn(1f, 32f) ?: 14f
        }.getOrElse { 16f }
}
