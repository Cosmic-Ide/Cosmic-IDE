package org.cosmic.ide.fragment.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import org.cosmic.ide.R
import org.cosmic.ide.ui.preference.IntListPreference
import org.cosmic.ide.ui.preference.showIntListPreferenceDialog

class AppearanceSettingsFragment :
    BasePreferenceFragment(R.string.pref_appearance),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_appearance)
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
        val context = requireActivity()

        when (key) {
            context.getString(R.string.key_theme) -> {
                AppCompatDelegate.setDefaultNightMode(settings.theme)
            }
        }
    }
}