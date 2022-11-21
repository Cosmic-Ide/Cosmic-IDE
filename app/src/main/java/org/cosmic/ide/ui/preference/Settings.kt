package org.cosmic.ide.ui.preference

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import org.cosmic.ide.App
import org.cosmic.ide.R

/**
 * CosmicIde's Settings.
 */
class Settings(private val context: Context) {
    private val `inner` = App.getDefaultPreferences()

    fun subscribe(listener: OnSharedPreferenceChangeListener) {
        `inner`.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unsubscribe(listener: OnSharedPreferenceChangeListener) {
        `inner`.unregisterOnSharedPreferenceChangeListener(listener)
    }

    val theme: Int
        get() =
            `inner`.getInt(
                context.getString(R.string.key_theme),
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )

    var javaVersion: Int
        get() =
            `inner`.getInt(
                context.getString(R.string.key_java_version),
                7
            )
        set(value) {
            `inner`.edit { putInt(context.getString(R.string.key_java_version), value) }
        }
}
