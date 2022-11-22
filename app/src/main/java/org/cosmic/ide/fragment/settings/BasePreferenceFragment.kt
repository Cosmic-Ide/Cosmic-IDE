package org.cosmic.ide.fragment.settings

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.PreferenceFragmentCompat
import org.cosmic.ide.ui.preference.Settings

abstract class BasePreferenceFragment(@StringRes private val titleId: Int) : PreferenceFragmentCompat() {

    val settings: Settings by lazy { Settings() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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