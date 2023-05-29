/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

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
