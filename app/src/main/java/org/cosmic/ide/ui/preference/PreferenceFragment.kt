package org.cosmic.ide.ui.preference

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.children
import org.cosmic.ide.R
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import org.cosmic.ide.util.Constants.DISCORD_URL
import org.cosmic.ide.util.Constants.GITHUB_RELEASE_URL
import org.cosmic.ide.util.Constants.GITHUB_URL

/**
 * The actual fragment containing the settings menu.
 */
class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager.onDisplayPreferenceDialogListener = this
        preferenceScreen.children.forEach(::setupPreference)

        // Make the RecyclerView edge-to-edge capable
        view.findViewById<androidx.recyclerview.widget.RecyclerView>(androidx.preference.R.id.recycler_view).apply {
            clipToPadding = false
            addSystemWindowInsetToPadding(false, false, false, true)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

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

    @Suppress("Deprecation")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is ListPreference -> {
                showListPreferenceDialog(preference)
            }
            is IntListPreference -> {
                showIntListPreferenceDialog(preference)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun setupPreference(preference: Preference) {
        val context = requireActivity()
        val settings = Settings(context)

        if (!preference.isVisible) return

        if (preference is PreferenceCategory) {
            preference.children.forEach(::setupPreference)
            return
        }

        when (preference.key) {
            context.getString(R.string.key_theme) -> {
                preference.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, value ->
                        AppCompatDelegate.setDefaultNightMode(value as Int)
                        true
                    }
            }
        }
    }
}
