package org.cosmic.ide.fragment.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import com.google.android.material.color.DynamicColors
import org.cosmic.ide.R
import org.cosmic.ide.ui.preference.IntListPreference
import org.cosmic.ide.ui.preference.showIntListPreferenceDialog

class AppearanceSettingsFragment :
    BasePreferenceFragment(R.string.appearance),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_appearance)
        findPreference<Preference>("dynamic_theme")?.isVisible = DynamicColors.isDynamicColorAvailable()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings.subscribe(this)
    }

    override fun onDestroyView() {
        settings.unsubscribe(this)
        super.onDestroyView()
    }

    @Suppress("Deprecation")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is IntListPreference -> {
                showIntListPreferenceDialog(preference)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        when (key) {
            "theme" ->  AppCompatDelegate.setDefaultNightMode(settings.theme)
            "dynamic_theme" -> {
                // At the moment I don't know how to recreate all activities on the back stack.
                // Temporary hack: Set current theme to current
                AppCompatDelegate.setDefaultNightMode(settings.theme)
            }
        }
    }

    // private fun postRestart() {
        // view?.postDelayed(400) {
            // requireActivity().recreate()
        // }
    // }
}