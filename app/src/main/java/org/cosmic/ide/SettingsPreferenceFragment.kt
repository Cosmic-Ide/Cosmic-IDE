package org.cosmic.ide

import android.os.Bundle

import com.takisoft.preferencex.PreferenceFragmentCompat

import org.cosmic.ide.ui.utils.CustomThemeHelper
import org.cosmic.ide.ui.utils.NightModeHelper

class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val viewLifecycleOwner = viewLifecycleOwner
        Settings.MD3.observe(viewLifecycleOwner, this::onMD3Changed)
        Settings.NIGHT_MODE.observe(viewLifecycleOwner, this::onNightModeChanged)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String ?) {
        addPreferencesFromResource(R.xml.settings)
    }

    private fun onMD3Changed(isMD3: Boolean) {
        CustomThemeHelper.sync()
    }

    private fun onNightModeChanged(nightMode: NightMode) {
        NightModeHelper.sync()
    }
}