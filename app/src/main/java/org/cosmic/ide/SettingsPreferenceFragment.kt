package org.cosmic.ide

import android.content.Intent
import android.os.Bundle

import androidx.core.net.toUri
import androidx.preference.Preference

import com.takisoft.preferencex.PreferenceFragmentCompat

import org.cosmic.ide.ui.utils.CustomThemeHelper
import org.cosmic.ide.ui.utils.DarkThemeHelper
import org.cosmic.ide.ui.utils.DarkTheme

class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val viewLifecycleOwner = viewLifecycleOwner
        Settings.MD3.observe(viewLifecycleOwner, this::onMD3Changed)
        Settings.DARK_THEME.observe(viewLifecycleOwner, this::onDarkThemeChanged)
    }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        findPreference<Preference>(ApplicationLoader.applicationContext().resources.getString(R.string.pref_key_app_version))?.setOnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Constants.APP_URL.toUri()))
            true
        }
    }

    private fun onMD3Changed(isMD3: Boolean) {
        CustomThemeHelper.sync()
    }

    private fun onDarkThemeChanged(darkTheme: DarkTheme) {
        DarkThemeHelper.sync()
    }
}