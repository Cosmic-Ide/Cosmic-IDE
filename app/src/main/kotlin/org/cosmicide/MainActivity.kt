/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.cosmicide.app.AppContainer
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.common.Prefs
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.lsp.LspEditorLanguageProvider
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.ui.IDENavigation
import org.cosmicide.ui.donation.DonationPromptTracker
import org.cosmicide.ui.editor.resolveTheme
import org.cosmicide.ui.theme.IDETheme
import org.cosmicide.ui.theme.isDeviceInDarkTheme
import org.cosmicide.util.PreferenceKeys
import org.eclipse.tm4e.core.registry.IThemeSource

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        if (savedInstanceState == null) {
            DonationPromptTracker.recordLaunch(applicationContext)
        }
        val appContainer = AppContainer(applicationContext)

        setContent {
            CompositionLocalProvider(LocalAppContainer provides appContainer) {
                IDETheme {
                    val colorScheme = MaterialTheme.colorScheme
                    val darkTheme = isDeviceInDarkTheme()
                    LaunchedEffect(colorScheme, darkTheme) {
                        loadEditorThemes(colorScheme, darkTheme)
                    }
                    IDENavigation()
                }
            }
        }
    }

    private fun loadEditorThemes(colorScheme: ColorScheme, darkTheme: Boolean) {
        val themeRegistry = ThemeRegistry.getInstance()

        arrayOf("darcula.json", "QuietLight.tmTheme.json").forEach { name ->
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

        val enabledThemes = CosmicPluginHost
            .enabledExtensions(EditorExtensionPoints.THEME_PROVIDER)
            .mapNotNull { provider ->
                val theme = runCatching { provider.createTheme() }
                    .onFailure {
                        Log.w(TAG, "Theme provider ${provider.id} failed to build its theme", it)
                    }
                    .getOrNull()
                theme?.let { provider to it }
            }

        enabledThemes.forEach { (_, theme) ->
            runCatching { themeRegistry.loadTheme(theme) }
                .onFailure {
                    Log.w(TAG, "Failed to register theme '${theme.name}'", it)
                }
        }

        applyEditorThemeSelection(themeRegistry, darkTheme)

        LspEditorLanguageProvider.updateColors(colorScheme)
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}

/**
 * Activates the editor theme selected in settings. The automatic option keeps the bundled
 * darcula/Quiet Light themes for the active dark/light mode.
 */
internal fun applyEditorThemeSelection(themeRegistry: ThemeRegistry, darkTheme: Boolean) {
    val requested = Prefs.editorTheme
    val applied = requested != PreferenceKeys.EDITOR_THEME_AUTO && themeRegistry.setTheme(requested)
    if (applied) return
    themeRegistry.setTheme(if (darkTheme) "darcula" else "QuietLight")
}
