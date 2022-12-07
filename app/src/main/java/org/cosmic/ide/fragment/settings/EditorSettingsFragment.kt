package org.cosmic.ide.fragment.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import org.cosmic.ide.R
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.ui.preference.SliderPreference

class EditorSettingsFragment :
    BasePreferenceFragment(R.string.editor),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_editor)

        findPreference<SliderPreference>(Settings.FONT_SIZE)?.run {
            summary = value.toString()
            setOnPreferenceChangeListener { preference, newValue ->
                preference.summary = newValue.toString()
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings.subscribe(this)
    }

    override fun onDestroyView() {
        settings.unsubscribe(this)
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        // TODO
    }
}
