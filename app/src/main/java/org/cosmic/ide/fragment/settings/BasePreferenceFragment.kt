package org.cosmic.ide.fragment.settings

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.PreferenceFragmentCompat
import org.cosmic.ide.ui.preference.Settings

abstract class BasePreferenceFragment(@StringRes private val titleId: Int) : PreferenceFragmentCompat() {

    lateinit var settings: Settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settings = Settings(requireContext())
        listView.clipToPadding = false
    }

    override fun onResume() {
        super.onResume()
        if (titleId != 0) {
            setTitle(getString(titleId))
        }
    }

    @Suppress("UsePropertyAccessSyntax")
    protected fun setTitle(title: CharSequence) {
        (parentFragment as? SettingsHeadersFragment)?.setTitle(title)
            ?: activity?.setTitle(title)
    }
}