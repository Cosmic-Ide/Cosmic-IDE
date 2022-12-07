package org.cosmic.ide.fragment.settings

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import org.cosmic.ide.ui.preference.Settings

abstract class BasePreferenceFragment(@StringRes private val titleId: Int) : PreferenceFragmentCompat() {

    lateinit var rootView: View
        private set

    val settings by lazy { Settings() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
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
        activity?.setTitle(title)
    }
}

fun BasePreferenceFragment.showSnackbar(message: String) =
    Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
