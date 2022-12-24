package org.cosmic.ide.ui.preference

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import com.google.android.material.color.DynamicColors
import org.cosmic.ide.App

/**
 * CosmicIde's Settings.
 */
class Settings {
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
                THEME,
                MODE_NIGHT_FOLLOW_SYSTEM
            )

    val isDynamicTheme: Boolean
        get() =
            DynamicColors.isDynamicColorAvailable() &&
                prefs.getBoolean(
                    DYNAMIC_THEME,
                    false
                )

    val programArguments: String
        get() =
            prefs.getString(
                PROGRAM_ARGUMENTS,
                ""
            )!!

    val fontSize: Int
        get() =
            prefs.getInt(
                FONT_SIZE,
                14
            )

    val javaVersion: Int
        get() =
            prefs.getInt(
                JAVA_VERSION,
                7
            )

    val gitUserName: String
        get() =
            prefs.getString(
                GIT_USERNAME,
                "User"
            )!!

    val gitUserEmail: String
        get() =
            prefs.getString(
                GIT_USEREMAIL,
                "user@localhost.com"
            )!!

    companion object {
        const val THEME = "theme"
        const val DYNAMIC_THEME = "dynamic_theme"
        const val PROGRAM_ARGUMENTS = "program_arguments"
        const val FONT_SIZE = "font_size"
        const val JAVA_VERSION = "java_version"
        const val GIT_USERNAME = "git_username"
        const val GIT_USEREMAIL = "git_useremail"
        const val DISCORD = "discord"
        const val GITHUB = "github"
        const val APP_VERSION = "app_version"
        const val OSS_LICENSES = "oss_licenses"
    }
}
