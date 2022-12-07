package org.cosmic.ide.fragment.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import com.google.android.material.color.DynamicColors
import org.cosmic.ide.R
import org.cosmic.ide.ui.preference.IntListPreference
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.ui.preference.showIntListPreferenceDialog

class AppearanceSettingsFragment :
    BasePreferenceFragment(R.string.appearance),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_appearance)
        findPreference<Preference>(Settings.DYNAMIC_THEME)?.isVisible = DynamicColors.isDynamicColorAvailable()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings.subscribe(this)
    }

    override fun onDestroyView() {
        settings.unsubscribe(this)
        super.onDestroyView()
    }

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
            Settings.THEME -> AppCompatDelegate.setDefaultNightMode(settings.theme)
            Settings.DYNAMIC_THEME -> {
                showSnackbar("Restart the application to apply the dynamic colors")
            }
        }
    }
}
