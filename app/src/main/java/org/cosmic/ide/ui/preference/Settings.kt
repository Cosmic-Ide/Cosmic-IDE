package org.cosmic.ide.ui.preference

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.core.content.edit
import com.google.android.material.color.DynamicColors
import org.cosmic.ide.App
import org.cosmic.ide.R

/**
 * CosmicIde's Settings.
 */
class Settings() {
    val prefs = App.getDefaultPreferences()

    fun subscribe(listener: OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unsubscribe(listener: OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    val theme: Int
        get() =
            prefs.getInt(
                "theme",
                MODE_NIGHT_FOLLOW_SYSTEM
            )

    val isDynamicTheme: Boolean
        get() =
            DynamicColors.isDynamicColorAvailable() &&
            prefs.getBoolean(
                "dynamic_theme",
                false
            )

    val fontSize: Int
        get() =
            prefs.getInt(
                "font_size",
                14
            )

    val javaVersion: Int
        get() =
            prefs.getInt(
                "java_version",
                7
            )

    val gitUserName: String
        get() =
            prefs.getString(
                "git_username",
                "User"
            )!!

    val gitUserEmail: String
        get() =
            prefs.getString(
                "git_useremail",
                "user@localhost.com"
            )!!
}
