package org.cosmicide.rewrite.common

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * A utility object to access shared preferences easily.
 */
object Prefs {
    @JvmField val useFastJarFs: Boolean = prefs.getBoolean("use_fastjarfs", false)
    @JvmField val useSpaces: Boolean = prefs.getBoolean("use_spaces", false)
    @JvmField val tabSize: Int = prefs.getInt("tab_size", 4)
    @JvmField val compilerJavaVersion: Int = prefs.getInt("java_version", 17)
    @JvmField val useSSVM: Boolean = prefs.getBoolean("use_ssvm", false)

    private lateinit var prefs: SharedPreferences

    /**
     * The font size selected by the user.
     */
    val editorFontSize: Float
        get() {
            return runCatching {
                val fontSizeString = prefs.getString("font_size", "16") ?: "16"
                fontSizeString.toFloat().coerceIn(1f, 32f)
            }.getOrDefault(16f)
        }

    /**
     * Initializes shared preferences.
     * @param context The context of the application.
     */
    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }
}