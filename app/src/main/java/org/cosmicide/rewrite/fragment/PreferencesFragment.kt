/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.app.UiModeManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.cosmicide.rewrite.BuildConfig
import org.cosmicide.rewrite.R

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
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val theme = getTheme(newValue as String)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        getSystemService(
                            requireContext(),
                            UiModeManager::class.java
                        )?.setApplicationNightMode(theme)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(if (theme == UiModeManager.MODE_NIGHT_AUTO) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else theme)
                    }
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
            "light" -> UiModeManager.MODE_NIGHT_NO
            "dark" -> UiModeManager.MODE_NIGHT_YES
            else -> UiModeManager.MODE_NIGHT_AUTO
        }
    }
}