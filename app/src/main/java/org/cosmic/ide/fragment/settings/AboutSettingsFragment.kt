package org.cosmic.ide.fragment.settings

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.preference.Preference
import org.cosmic.ide.BuildConfig
import org.cosmic.ide.R
import org.cosmic.ide.util.Constants.DISCORD_URL
import org.cosmic.ide.util.Constants.GITHUB_RELEASE_URL
import org.cosmic.ide.util.Constants.GITHUB_URL

class AboutSettingsFragment : BasePreferenceFragment(R.string.pref_about) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_about)

        findPreference<Preference>("ide_app_version")?.run {
            title = getString(R.string.pref_app_version, BuildConfig.VERSION_NAME)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            "ide_app_version" -> {
                startActivity(Intent(ACTION_VIEW, GITHUB_RELEASE_URL.toUri()))
                true
            }
            "ide_discord_link" -> {
                startActivity(Intent(ACTION_VIEW, DISCORD_URL.toUri()))
                true
            }
            "ide_github_link" -> {
                startActivity(Intent(ACTION_VIEW, GITHUB_URL.toUri()))
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}