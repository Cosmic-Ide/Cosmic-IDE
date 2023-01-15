package org.cosmic.ide.fragment.settings

import android.os.Bundle
import androidx.preference.PreferenceScreen
import org.cosmic.ide.R

class RootSettingsFragment : BasePreferenceFragment(R.string.settings) {

    // val generalWords = setOf(getString(R.string.projects_directory))
    // val appearanceWords = setOf(getString(R.string.theme), getString(R.string.dynamic_theme))
    // val compilerWords = setOf(getString(R.string.java_version), getString(R.string.program_arguments), getString(R.string.fast_jar_fs))
    // val editorWords = setOf(getString(R.string.font_size))
    // val aboutWords = setOf("Version", getString(R.string.app_translation), getString(R.string.discord), getString(R.string.github), "Licenses")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_root)

        // findPreference<PreferenceScreen>("general")?.run {
            // summary = generalWords.split("\\s+".toRegex())
        // }

        // findPreference<PreferenceScreen>("appearance")?.run {
            // summary = appearanceWords.split("\\s+".toRegex())
        // }

        // findPreference<PreferenceScreen>("compiler")?.run {
            // summary = compilerWords.split("\\s+".toRegex())
        // }

        // findPreference<PreferenceScreen>("editor")?.run {
            // summary = editorWords.split("\\s+".toRegex())
        // }

        // findPreference<PreferenceScreen>("about")?.run {
            // summary = aboutWords.split("\\s+".toRegex())
        // }
    }
}
