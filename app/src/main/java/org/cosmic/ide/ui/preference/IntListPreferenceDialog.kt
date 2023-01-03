package org.cosmic.ide.ui.preference

import android.os.Bundle
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class IntListPreferenceDialog : PreferenceDialogFragmentCompat() {
    private val listPreference: IntListPreference
        get() = (preference as IntListPreference)
    private var pendingValueIndex = -1

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        // PreferenceDialogFragmentCompat does not allow us to customize the actual creation
        // of the alert dialog, so we have to manually override onCreateDialog and customize it
        // ourselves.
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(listPreference.title)
            .setPositiveButton(null, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setSingleChoiceItems(listPreference.entries, listPreference.getValueIndex()) { _, index
                ->
                pendingValueIndex = index
                dismiss()
            }
            .create()

    override fun onDialogClosed(positiveResult: Boolean) {
        if (pendingValueIndex > -1) {
            listPreference.setValueIndex(pendingValueIndex)
        }
    }
}

fun PreferenceFragmentCompat.showIntListPreferenceDialog(preference: IntListPreference) {
    val dialogFragment = IntListPreferenceDialog().apply {
        arguments = Bundle(1).apply {
            putString("key", preference.key)
        }
    }
    @Suppress("DEPRECATION")
    dialogFragment.setTargetFragment(this, 0)
    dialogFragment.show(
        parentFragmentManager,
        "androidx.preference.PreferenceFragment.IntListPreferenceDialog"
    )
}
