package org.cosmicide.extension

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.TypedValue
import androidx.core.content.ContextCompat

fun Context.copyToClipboard(text: String) {
    val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)!!
    clipboard.setPrimaryClip(ClipData.newPlainText("", text))
}

fun Context.getDip(input: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, input, resources.displayMetrics)
}