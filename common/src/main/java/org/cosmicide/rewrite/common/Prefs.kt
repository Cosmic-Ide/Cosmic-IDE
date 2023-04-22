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
     * The font size selected by the user.
     */
    val editorFontSize: Float
        get() {
            return try {
                prefs.getString("font_size", "16")?.toFloat() ?: 16f
            } catch (e: Exception) {
                16f
            }
        }

    /**
     * The Java version selected by the user.
     */
    val compilerJavaVersion: Int
        get() {
            return try {
                prefs.getInt("java_version", 17)
            } catch (e: Exception) {
                17
            }
        }

    /**
     * The FastJarFs selected by user.
     */
    val useFastJarFs: Boolean
        get() = prefs.getBoolean("use_fastjarfs", false)

    val useSpaces: Boolean
        get() = prefs.getBoolean("use_spaces", false)

    val tabSize: Int
        get() = prefs.getInt("tab_size", 4)

    val useSSVM: Boolean
        get() = prefs.getBoolean("use_ssvm", false)

    /**
     * Initializes shared preferences.
     * @param context The context of the application.
     */
    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }
}