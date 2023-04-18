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
                prefs.getString("font_size", "12")?.toFloat() ?: 12f
            } catch (e: Exception) {
                12f
            }
        }

    /**
     * The Java version selected by the user.
     */
    val compilerJavaVersion: Int
        get() {
            return try {
                prefs.getString("java_version", "17")?.toInt() ?: 17
            } catch (e: Exception) {
                17
            }
        }

    /**
     * The FastJarFs selected by user.
     */
    val useFastJarFs: Boolean
        get() = prefs.getBoolean("use_fastjarfs", false)

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