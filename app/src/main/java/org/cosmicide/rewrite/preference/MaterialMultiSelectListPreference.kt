package org.cosmicide.rewrite.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialMultiSelectListPreference(context: Context, attrs: AttributeSet) :
    MultiSelectListPreference(context, attrs) {

    override fun onClick() {
        val selectedValues = entryValues?.map { it in values }

        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder
            .setTitle(title)
            .setMultiChoiceItems(entries, selectedValues?.toBooleanArray()) { _, which, isChecked ->
                val value = entryValues?.get(which)?.toString()
                if (isChecked) {
                    values.add(value)
                } else {
                    values.remove(value)
                }
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                persistStringSet(values)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
