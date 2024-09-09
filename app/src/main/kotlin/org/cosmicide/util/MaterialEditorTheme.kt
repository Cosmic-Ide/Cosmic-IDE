/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.cosmicide.extension.getDynamicColor
import java.io.InputStream

object MaterialEditorTheme {
    private val gson = Gson()

    fun resolveTheme(context: Context, fileName: String): InputStream {
        val theme = context.assets.open("textmate/$fileName")
        return applyAttributes(theme, context)
    }

    fun applyAttributes(stream: InputStream, context: Context): InputStream {
        val contents = stream.bufferedReader().readText()

        val json = gson.fromJson(contents, Map::class.java)
        // Should probably clean this up
        ((json["settings"]!! as List<Map<String, Any>>)[0]["settings"]!! as MutableMap<String, String>).let { settings ->
            settings["background"] =
                context.getDynamicColor(com.google.android.material.R.attr.colorSurfaceContainerLow)
            settings["foreground"] =
                context.getDynamicColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
            settings["caret"] =
                context.getDynamicColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        }
        Log.d("MaterialEditorTheme", "Applying attributes to theme")
        Log.d("MaterialEditorTheme", json.toString())

        return gson.toJson(json).byteInputStream()
    }
}
