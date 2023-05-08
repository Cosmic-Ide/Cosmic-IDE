package org.cosmicide.rewrite.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object CommonUtils {
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(clip)
    }
}