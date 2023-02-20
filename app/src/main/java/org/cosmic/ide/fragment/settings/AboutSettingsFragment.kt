package org.cosmic.ide.fragment.settings

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import androidx.core.net.toUri
import androidx.preference.Preference
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import org.cosmic.ide.R
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.util.Constants.DISCORD_URL
import org.cosmic.ide.util.Constants.GITHUB_RELEASE_URL
import org.cosmic.ide.util.Constants.GITHUB_URL

class AboutSettingsFragment : BasePreferenceFragment(R.string.about) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_about)

        findPreference<Preference>(Settings.APP_VERSION)?.run {
            // set title to app version
            title = getString(
                R.string.app_version,
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            )
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            Settings.APP_VERSION -> {
                startActivity(Intent(ACTION_VIEW, GITHUB_RELEASE_URL.toUri()))
                true
            }
            Settings.DISCORD -> {
                startActivity(Intent(ACTION_VIEW, DISCORD_URL.toUri()))
                true
            }
            Settings.GITHUB -> {
                startActivity(Intent(ACTION_VIEW, GITHUB_URL.toUri()))
                true
            }
            Settings.OSS_LICENSES -> {
                startActivity(Intent(activity, OssLicensesMenuActivity::class.java))
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}
