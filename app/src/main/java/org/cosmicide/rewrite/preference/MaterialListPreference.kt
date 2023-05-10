package org.cosmicide.rewrite.preference

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.ListPreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * A custom [ListPreferenceDialogFragmentCompat] that uses MaterialAlertDialogBuilder to display the dialog.
 */
class MaterialListPreference : ListPreferenceDialogFragmentCompat() {

    private var whichButtonClicked = 0
    private var onDialogClosedWasCalledFromOnDismiss = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        whichButtonClicked = DialogInterface.BUTTON_NEGATIVE

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(preference.dialogTitle)
            .setIcon(preference.dialogIcon)
            .setPositiveButton(preference.positiveButtonText, this)
            .setNegativeButton(preference.negativeButtonText, this)
            .apply {
                val contentView = onCreateDialogView(requireContext())
                if (contentView != null) {
                    onBindDialogView(contentView)
                    setView(contentView)
                } else {
                    setMessage(preference.dialogMessage)
                }
                onPrepareDialogBuilder(this)
            }

        return builder.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        whichButtonClicked = which
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDialogClosedWasCalledFromOnDismiss = true
        super.onDismiss(dialog)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (onDialogClosedWasCalledFromOnDismiss) {
            onDialogClosedWasCalledFromOnDismiss = false
            super.onDialogClosed(whichButtonClicked == DialogInterface.BUTTON_POSITIVE)
        } else {
            super.onDialogClosed(positiveResult)
        }
    }
}

fun PreferenceFragmentCompat.showListPreference(preference: ListPreference) {
    val fragment = MaterialListPreference().apply {
        arguments = Bundle().apply {
            putString("key", preference.key)
        }
    }

    fragment.setTargetFragment(this, 0)
    fragment.show(parentFragmentManager, "MaterialListPreference")
}