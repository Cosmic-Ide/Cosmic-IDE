package org.cosmicide.rewrite.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.ContextCompat

object CommonUtils {

    @JvmStatic
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)!!
        clipboard.setPrimaryClip(ClipData.newPlainText("", text))
    }
}