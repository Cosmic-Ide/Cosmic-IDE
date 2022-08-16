package org.cosmic.ide.preference

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle

import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.ListPreference

import com.takisoft.preferencex.PreferenceFragmentCompat

import org.cosmic.ide.R
import org.cosmic.ide.ApplicationLoader
import org.cosmic.ide.Constants.GITHUB_URL
import org.cosmic.ide.Constants.DISCORD_URL
import org.cosmic.ide.ui.theme.CustomThemeHelper
import org.cosmic.ide.ui.theme.DarkThemeHelper
import org.cosmic.ide.ui.theme.DarkTheme

class SettingsPreferenceFragment : PreferenceFragmentCompat() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val viewLifecycleOwner = viewLifecycleOwner
        Settings.MD3.observe(viewLifecycleOwner, this::onMD3Changed)
        Settings.DARK_THEME.observe(viewLifecycleOwner, this::onDarkThemeChanged)
    }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        findPreference<Preference>(ApplicationLoader.applicationContext().getString(R.string.pref_key_app_version))?.setOnPreferenceClickListener {
            startActivity(Intent(ACTION_VIEW, GITHUB_URL.toUri()))
            true
        }
        findPreference<Preference>(ApplicationLoader.applicationContext().getString(R.string.pref_key_discord))?.setOnPreferenceClickListener {
            startActivity(Intent(ACTION_VIEW, DISCORD_URL.toUri()))
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ListPreference) {
            showListPreferenceDialog(preference)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun onMD3Changed(isMD3: Boolean) {
        CustomThemeHelper.sync()
    }

    private fun onDarkThemeChanged(darkTheme: DarkTheme) {
        DarkThemeHelper.sync()
    }
}