package org.cosmic.ide.fragment.settings

import android.os.Bundle
import org.cosmic.ide.R

class RootSettingsFragment : BasePreferenceFragment(R.string.settings) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_root)
    }
}
