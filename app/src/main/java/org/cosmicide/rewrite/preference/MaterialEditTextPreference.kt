/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MaterialEditTextPreference(context: Context, attrs: AttributeSet) :
    EditTextPreference(context, attrs) {

    override fun onClick() {
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        val editText = TextInputEditText(context)
        editText.setText(getPersistedString(""))
        dialogBuilder
            .setTitle(title)
            .setView(TextInputLayout(context).apply {
                addView(editText)
                setPadding(8, 4, 8, 4)
            })
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                persistString(editText.text.toString())
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}