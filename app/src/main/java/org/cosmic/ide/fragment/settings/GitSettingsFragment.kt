package org.cosmic.ide.fragment.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.cosmic.ide.R

class GitSettingsFragment :
    BasePreferenceFragment(R.string.git),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_git)
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
        TODO("Not yet implemented")
    }
}