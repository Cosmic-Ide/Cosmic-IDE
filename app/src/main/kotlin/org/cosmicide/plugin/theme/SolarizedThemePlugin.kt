/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.theme

import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.EditorThemeProvider
import org.cosmicide.plugin.api.CosmicPlugin
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.PluginDescriptor
import org.eclipse.tm4e.core.registry.IThemeSource

class SolarizedThemePlugin : CosmicPlugin {
    override fun activate(context: PluginContext) {
        val owner = context.descriptor.id
        context.registerDisposable(
            context.extensions.register(
                point = EditorExtensionPoints.THEME_PROVIDER,
                extension = SolarizedDarkThemeProvider,
                ownerPluginId = owner,
                priority = SolarizedDarkThemeProvider.priority
            )
        )
        context.registerDisposable(
            context.extensions.register(
                point = EditorExtensionPoints.THEME_PROVIDER,
                extension = SolarizedLightThemeProvider,
                ownerPluginId = owner,
                priority = SolarizedLightThemeProvider.priority
            )
        )
    }

    companion object {
        const val PLUGIN_ID = "org.cosmicide.theme.solarized"

        val descriptor = PluginDescriptor(
            id = PLUGIN_ID,
            name = "Solarized themes",
            version = "1.0.0",
            entryClass = SolarizedThemePlugin::class.java.name,
            description = "Solarized dark and light color themes for the code editor.",
            author = "Cosmic IDE",
            capabilities = setOf("editor.theme")
        )
    }
}

object SolarizedDarkThemeProvider : EditorThemeProvider {
    override val id = "org.cosmicide.theme.solarized.dark"
    override val displayName = "Solarized Dark"
    override val description = "Solarized dark color scheme for the code editor."
    override val priority = 100
    override val isDark = true

    override fun createTheme(): ThemeModel =
        ThemeModel(
            IThemeSource.fromString(IThemeSource.ContentType.JSON, SOLARIZED_DARK_JSON),
            SOLARIZED_DARK_NAME
        ).apply { isDark = true }
}

object SolarizedLightThemeProvider : EditorThemeProvider {
    override val id = "org.cosmicide.theme.solarized.light"
    override val displayName = "Solarized Light"
    override val description = "Solarized light color scheme for the code editor."
    override val priority = 100
    override val isDark = false

    override fun createTheme(): ThemeModel =
        ThemeModel(
            IThemeSource.fromString(IThemeSource.ContentType.JSON, SOLARIZED_LIGHT_JSON),
            SOLARIZED_LIGHT_NAME
        ).apply { isDark = false }
}

private const val SOLARIZED_DARK_NAME = "Solarized Dark"
private const val SOLARIZED_LIGHT_NAME = "Solarized Light"

private val SOLARIZED_DARK_JSON = """
    {
      "name": "$SOLARIZED_DARK_NAME",
      "settings": [
        {
          "settings": {
            "background": "#002b36",
            "foreground": "#839496",
            "caret": "#d33682",
            "selection": "#073642",
            "lineHighlight": "#073642",
            "inactiveSelection": "#073642"
          }
        },
        { "scope": "comment", "settings": { "foreground": "#586e75", "fontStyle": "italic" } },
        { "scope": ["keyword", "keyword.control", "keyword.operator", "storage"], "settings": { "foreground": "#859900" } },
        { "scope": ["storage.type", "entity.name.type", "entity.name.class", "entity.name.struct", "support.type"], "settings": { "foreground": "#b58900" } },
        { "scope": ["entity.name.function", "entity.name.method", "support.function"], "settings": { "foreground": "#268bd2" } },
        { "scope": ["variable", "variable.language", "variable.parameter"], "settings": { "foreground": "#268bd2" } },
        { "scope": ["constant", "constant.language", "constant.numeric", "constant.character"], "settings": { "foreground": "#d33682" } },
        { "scope": ["constant.character.escape", "string.escape"], "settings": { "foreground": "#cb4b16" } },
        { "scope": ["string", "string.quoted", "support.constant", "constant.other"], "settings": { "foreground": "#2aa198" } },
        { "scope": ["punctuation", "punctuation.definition", "meta.brace"], "settings": { "foreground": "#839496" } },
        { "scope": ["punctuation.separator", "punctuation.definition.tag"], "settings": { "foreground": "#93a1a1" } },
        { "scope": ["entity.name.tag", "meta.tag"], "settings": { "foreground": "#268bd2" } },
        { "scope": "entity.other.attribute-name", "settings": { "foreground": "#b58900" } },
        { "scope": ["invalid", "invalid.illegal"], "settings": { "foreground": "#dc322f" } },
        { "scope": "markup.heading", "settings": { "foreground": "#268bd2", "fontStyle": "bold" } },
        { "scope": "markup.bold", "settings": { "fontStyle": "bold" } },
        { "scope": "markup.italic", "settings": { "fontStyle": "italic" } },
        { "scope": "markup.quote", "settings": { "foreground": "#2aa198" } },
        { "scope": "markup.link", "settings": { "foreground": "#93a1a1", "fontStyle": "underline" } },
        { "scope": "markup.deleted", "settings": { "foreground": "#dc322f" } },
        { "scope": "markup.inserted", "settings": { "foreground": "#859900" } },
        { "scope": "markup.changed", "settings": { "foreground": "#cb4b16" } }
      ]
    }
""".trimIndent()

private val SOLARIZED_LIGHT_JSON = """
    {
      "name": "$SOLARIZED_LIGHT_NAME",
      "settings": [
        {
          "settings": {
            "background": "#fdf6e3",
            "foreground": "#657b83",
            "caret": "#d33682",
            "selection": "#eee8d5",
            "lineHighlight": "#eee8d5",
            "inactiveSelection": "#eee8d5"
          }
        },
        { "scope": "comment", "settings": { "foreground": "#93a1a1", "fontStyle": "italic" } },
        { "scope": ["keyword", "keyword.control", "keyword.operator", "storage"], "settings": { "foreground": "#859900" } },
        { "scope": ["storage.type", "entity.name.type", "entity.name.class", "entity.name.struct", "support.type"], "settings": { "foreground": "#b58900" } },
        { "scope": ["entity.name.function", "entity.name.method", "support.function"], "settings": { "foreground": "#268bd2" } },
        { "scope": ["variable", "variable.language", "variable.parameter"], "settings": { "foreground": "#268bd2" } },
        { "scope": ["constant", "constant.language", "constant.numeric", "constant.character"], "settings": { "foreground": "#d33682" } },
        { "scope": ["constant.character.escape", "string.escape"], "settings": { "foreground": "#cb4b16" } },
        { "scope": ["string", "string.quoted", "support.constant", "constant.other"], "settings": { "foreground": "#2aa198" } },
        { "scope": ["punctuation", "punctuation.definition", "meta.brace"], "settings": { "foreground": "#657b83" } },
        { "scope": ["punctuation.separator", "punctuation.definition.tag"], "settings": { "foreground": "#586e75" } },
        { "scope": ["entity.name.tag", "meta.tag"], "settings": { "foreground": "#268bd2" } },
        { "scope": "entity.other.attribute-name", "settings": { "foreground": "#b58900" } },
        { "scope": ["invalid", "invalid.illegal"], "settings": { "foreground": "#dc322f" } },
        { "scope": "markup.heading", "settings": { "foreground": "#268bd2", "fontStyle": "bold" } },
        { "scope": "markup.bold", "settings": { "fontStyle": "bold" } },
        { "scope": "markup.italic", "settings": { "fontStyle": "italic" } },
        { "scope": "markup.quote", "settings": { "foreground": "#2aa198" } },
        { "scope": "markup.deleted", "settings": { "foreground": "#dc322f" } },
        { "scope": "markup.inserted", "settings": { "foreground": "#859900" } },
        { "scope": "markup.changed", "settings": { "foreground": "#cb4b16" } }
      ]
    }
""".trimIndent()
