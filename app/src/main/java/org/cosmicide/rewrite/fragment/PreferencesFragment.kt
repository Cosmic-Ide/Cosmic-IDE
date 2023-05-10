package org.cosmicide.rewrite.fragment

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentTransaction
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.cosmicide.rewrite.BuildConfig
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.preference.showListPreference

/**
 * A [PreferenceFragmentCompat] subclass to display the preferences UI.
 */
class PreferencesFragment : PreferenceFragmentCompat() {

    companion object {
        const val KEY_APP_THEME = "app_theme"
        const val KEY_VERSION = "version"
        const val KEY_PLUGINS = "plugins"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        findPreference<Preference>(KEY_APP_THEME)?.let { themePreference ->
            themePreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    AppCompatDelegate.setDefaultNightMode(getTheme(newValue as String))
                    true
                }
        }

        findPreference<Preference>(KEY_VERSION)?.apply {
            summary = BuildConfig.VERSION_NAME
        }

        findPreference<Preference>(KEY_PLUGINS)?.let { pluginsPreference ->
            pluginsPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    add(R.id.fragment_container, PluginsFragment())
                    addToBackStack(null)
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                }.commit()

                true
            }
        }
    }

    private fun getTheme(value: String): Int {
        return when (value) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is ListPreference -> showListPreference(preference)
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }
}