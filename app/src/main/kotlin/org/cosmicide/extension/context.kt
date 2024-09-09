/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.extension

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import okhttp3.internal.toHexString


fun Context.copyToClipboard(text: String) {
    val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)!!
    clipboard.setPrimaryClip(ClipData.newPlainText("", text))
}

fun Context.getDip(input: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, input, resources.displayMetrics)
}

fun Context.getDynamicColor(colorId: Int): String {
    return "#" + MaterialColors.getColor(this, colorId, null).toHexString()
}
