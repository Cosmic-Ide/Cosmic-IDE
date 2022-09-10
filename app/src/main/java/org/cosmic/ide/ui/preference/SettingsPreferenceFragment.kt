package org.cosmic.ide.ui.preference

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.takisoft.preferencex.PreferenceFragmentCompat
import org.cosmic.ide.R
import org.cosmic.ide.ui.theme.DarkTheme
import org.cosmic.ide.ui.theme.DarkThemeHelper
import org.cosmic.ide.util.Constants.DISCORD_URL
import org.cosmic.ide.util.Constants.GITHUB_RELEASE_URL
import org.cosmic.ide.util.Constants.GITHUB_URL

class SettingsPreferenceFragment : PreferenceFragmentCompat() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val viewLifecycleOwner = viewLifecycleOwner
        Settings.DARK_THEME.observe(viewLifecycleOwner, this::onDarkThemeChanged)
    }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        findPreference<Preference>("key_app_version")?.setOnPreferenceClickListener {
            startActivity(Intent(ACTION_VIEW, GITHUB_RELEASE_URL.toUri()))
            true
        }
        findPreference<Preference>("key_discord")?.setOnPreferenceClickListener {
            startActivity(Intent(ACTION_VIEW, DISCORD_URL.toUri()))
            true
        }

        findPreference<Preference>("key_github")?.setOnPreferenceClickListener {
            startActivity(Intent(ACTION_VIEW, GITHUB_URL.toUri()))
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

    private fun onDarkThemeChanged(darkTheme: DarkTheme) {
        DarkThemeHelper.sync()
    }
}
