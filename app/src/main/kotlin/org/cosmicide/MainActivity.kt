/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.cosmicide.ui.IDENavigation
import org.cosmicide.ui.editor.resolveTheme
import org.cosmicide.ui.theme.IDETheme
import org.cosmicide.ui.theme.isDeviceInDarkTheme
import org.eclipse.tm4e.core.registry.IThemeSource

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            IDETheme {
                val isDarkTheme = isDeviceInDarkTheme()
                loadEditorThemes(MaterialTheme.colorScheme)

                if (isDarkTheme) {
                    ThemeRegistry.getInstance().setTheme("darcula")
                } else {
                    ThemeRegistry.getInstance().setTheme("light")
                }

                IDENavigation()
            }
        }
    }

    private fun loadEditorThemes(colorScheme: ColorScheme) {
        val themes = arrayOf("darcula.json", "QuietLight.tmTheme.json")
        val themeRegistry = ThemeRegistry.getInstance()

        themes.forEach { name ->
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        resolveTheme(this, colorScheme, name), name, null
                    ), name.substringBefore('.')
                ).apply {
                    isDark = name.substringBefore('.') == "darcula"
                }
            )
        }
    }
}
