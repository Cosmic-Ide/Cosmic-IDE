package org.cosmicide.ui.editor

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.Gson
import java.io.InputStream

private val gson = Gson()

fun resolveTheme(context: Context, colorScheme: ColorScheme, fileName: String): InputStream {
    val theme = context.assets.open("textmate/$fileName")
    return applyAttributes(theme, colorScheme)
}

@Suppress("UNCHECKED_CAST")
fun applyAttributes(stream: InputStream, colorScheme: ColorScheme): InputStream {
    val contents = stream.bufferedReader().readText()

    val json = gson.fromJson(contents, Map::class.java)

    ((json["settings"]!! as List<Map<String, Any>>)[0]["settings"]!! as MutableMap<String, String>).let { settings ->
        settings["background"] =
            colorScheme.surface.hexString()
        settings["foreground"] =
            colorScheme.onSecondaryContainer.hexString()
        settings["blockLineColor"] =
            colorScheme.primary.hexString()
        settings["lineHighlight"] =
            colorScheme.surfaceContainer.hexString()
        settings["selection"] =
            colorScheme.primaryContainer.hexString()
        settings["caret"] =
            colorScheme.primary.hexString()
    }

    return gson.toJson(json).byteInputStream()
}

private fun Color.hexString(): String {
    val hex = String.format("#%06X", 0xFFFFFF and toArgb())
    return hex
}