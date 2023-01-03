/**
 * Taken as is from: https://stackoverflow.com/a/61424867/3969362
 * Modified by @TheRemakerMan
 */

package org.cosmic.ide.ui.preference

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.ListPreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialListPreferenceDialog : ListPreferenceDialogFragmentCompat() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(preference.dialogTitle)
            .setIcon(preference.dialogIcon)
            .setPositiveButton(preference.positiveButtonText, this)
            .setNegativeButton(preference.negativeButtonText, this)
        val contentView = onCreateDialogView(requireContext())
        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(preference.dialogMessage)
        }
        onPrepareDialogBuilder(builder)

        return builder.create()
    }

    /* Override the methods that access mWhichButtonClicked (because we cannot set it properly here) */

    // Which button was clicked.
    private var mWhichButtonClicked = 0

    override fun onClick(dialog: DialogInterface, which: Int) {
        mWhichButtonClicked = which
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDialogClosedWasCalledFromOnDismiss = true
        super.onDismiss(dialog)
    }

    private var onDialogClosedWasCalledFromOnDismiss = false

    override fun onDialogClosed(positiveResult: Boolean) {
        if (onDialogClosedWasCalledFromOnDismiss) {
            onDialogClosedWasCalledFromOnDismiss = false
            // this means the positiveResult needs to be calculated from our mWhichButtonClicked
            super.onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)
        } else {
            super.onDialogClosed(positiveResult)
        }
    }
}

fun PreferenceFragmentCompat.showListPreferenceDialog(preference: ListPreference) {
    val dialogFragment = MaterialListPreferenceDialog().apply {
        arguments = Bundle(1).apply {
            putString("key", preference.key)
        }
    }
    @Suppress("DEPRECATION")
    dialogFragment.setTargetFragment(this, 0)
    dialogFragment.show(
        parentFragmentManager,
        "androidx.preference.PreferenceFragment.MaterialListPreferenceDialog"
    )
}
