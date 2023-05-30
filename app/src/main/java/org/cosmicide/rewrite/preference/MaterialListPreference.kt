package org.cosmicide.rewrite.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialListPreference(context: Context, attrs: AttributeSet) :
    ListPreference(context, attrs) {

    override fun onClick() {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setSingleChoiceItems(entries, entryValues.indexOf(value)) { _, which ->
                value = entryValues[which].toString()
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                persistString(value)
                callChangeListener(value)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
