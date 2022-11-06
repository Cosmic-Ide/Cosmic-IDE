package org.cosmic.ide.ui.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import org.cosmic.ide.App
import org.cosmic.ide.R

/**
 * CosmicIde's Settings.
 */
class Settings(private val context: Context, private val callback: Callback? = null) :
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val inner = App.getDefaultSharedPreferences()

    interface Callback {
        fun onSettingChanged(key: String)
    }

    init {
        if (callback != null) {
            inner.registerOnSharedPreferenceChangeListener(this)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        callback!!.onSettingChanged(key)
    }

    fun release() {
        inner.unregisterOnSharedPreferenceChangeListener(this)
    }

    // The current theme
    val theme: Int
        get() =
            inner.getInt(
                context.getString(R.string.key_theme),
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )

    val javaVersion: Int
        get() =
            inner.getInt(
                context.getString(R.string.key_java_version),
                7
            )
}
