/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.theme

import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.EditorThemeProvider
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.PluginIds
import org.eclipse.tm4e.core.registry.IThemeSource

fun registerBuiltinThemeProviders(registry: MutableExtensionRegistry) {
    listOf(
        OneDarkThemeProvider, NordThemeProvider, DraculaThemeProvider, MonokaiThemeProvider
    ).forEach { provider ->
        registry.register(
            point = EditorExtensionPoints.THEME_PROVIDER,
            extension = provider,
            ownerPluginId = PluginIds.CORE,
            priority = provider.priority
        )
    }
}

object OneDarkThemeProvider : EditorThemeProvider {
    override val id = "org.cosmicide.editor.theme.one-dark"
    override val displayName = "One Dark"
    override val description = "Atom One Dark color scheme for the code editor."
    override val isDark = true
    override val canDisable = false

    override fun createTheme(): ThemeModel = themeModel(
        name = "One Dark",
        palette = Palette(
            background = "#282c34",
            foreground = "#abb2bf",
            caret = "#528bff",
            selection = "#3e4451",
            lineHighlight = "#2c313c",
            comment = "#5c6370",
            keyword = "#c678dd",
            type = "#e5c07b",
            function = "#61afef",
            variable = "#e06c75",
            constant = "#d19a66",
            string = "#98c379",
            escape = "#56b6c2",
            punctuation = "#abb2bf",
            separator = "#5c6370",
            tag = "#e06c75",
            attribute = "#d19a66",
            invalid = "#f44747"
        )
    )
}

object NordThemeProvider : EditorThemeProvider {
    override val id = "org.cosmicide.editor.theme.nord"
    override val displayName = "Nord"
    override val description = "Nord color scheme for the code editor."
    override val isDark = true
    override val canDisable = false

    override fun createTheme(): ThemeModel = themeModel(
        name = "Nord",
        palette = Palette(
            background = "#2e3440",
            foreground = "#d8dee9",
            caret = "#88c0d0",
            selection = "#434c5e",
            lineHighlight = "#3b4252",
            comment = "#616e88",
            keyword = "#81a1c1",
            type = "#8fbcbb",
            function = "#88c0d0",
            variable = "#d8dee9",
            constant = "#d08770",
            string = "#a3be8c",
            escape = "#ebcb8b",
            punctuation = "#d8dee9",
            separator = "#616e88",
            tag = "#81a1c1",
            attribute = "#d08770",
            invalid = "#bf616a"
        )
    )
}

object DraculaThemeProvider : EditorThemeProvider {
    override val id = "org.cosmicide.editor.theme.dracula"
    override val displayName = "Dracula"
    override val description = "Dracula color scheme for the code editor."
    override val isDark = true
    override val canDisable = false

    override fun createTheme(): ThemeModel = themeModel(
        name = "Dracula",
        palette = Palette(
            background = "#282a36",
            foreground = "#f8f8f2",
            caret = "#f8f8f2",
            selection = "#44475a",
            lineHighlight = "#313341",
            comment = "#6272a4",
            keyword = "#ff79c6",
            type = "#8be9fd",
            function = "#50fa7b",
            variable = "#f8f8f2",
            constant = "#bd93f9",
            string = "#f1fa8c",
            escape = "#ffb86c",
            punctuation = "#f8f8f2",
            separator = "#6272a4",
            tag = "#ff79c6",
            attribute = "#50fa7b",
            invalid = "#ff5555"
        )
    )
}

object MonokaiThemeProvider : EditorThemeProvider {
    override val id = "org.cosmicide.editor.theme.monokai"
    override val displayName = "Monokai"
    override val description = "Monokai color scheme for the code editor."
    override val isDark = true
    override val canDisable = false

    override fun createTheme(): ThemeModel = themeModel(
        name = "Monokai",
        palette = Palette(
            background = "#272822",
            foreground = "#f8f8f2",
            caret = "#f8f8f0",
            selection = "#49483e",
            lineHighlight = "#2e2e27",
            comment = "#75715e",
            keyword = "#f92672",
            type = "#66d9ef",
            function = "#a6e22e",
            variable = "#f8f8f2",
            constant = "#ae81ff",
            string = "#e6db74",
            escape = "#fd971f",
            punctuation = "#f8f8f2",
            separator = "#75715e",
            tag = "#f92672",
            attribute = "#a6e22e",
            invalid = "#fd971f"
        )
    )
}

private data class Palette(
    val background: String,
    val foreground: String,
    val caret: String,
    val selection: String,
    val lineHighlight: String,
    val comment: String,
    val keyword: String,
    val type: String,
    val function: String,
    val variable: String,
    val constant: String,
    val string: String,
    val escape: String,
    val punctuation: String,
    val separator: String,
    val tag: String,
    val attribute: String,
    val invalid: String
)

private fun themeModel(name: String, palette: Palette): ThemeModel =
    ThemeModel(
        IThemeSource.fromString(IThemeSource.ContentType.JSON, themeJson(name, palette)),
        name
    ).apply { isDark = true }

private fun themeJson(name: String, p: Palette): String = buildString {
    appendLine("{")
    appendLine("""  "name": "$name",""")
    appendLine("  \"settings\": [")
    appendLine("    {")
    appendLine("      \"settings\": {")
    appendLine("        \"background\": \"${p.background}\",")
    appendLine("        \"foreground\": \"${p.foreground}\",")
    appendLine("        \"caret\": \"${p.caret}\",")
    appendLine("        \"selection\": \"${p.selection}\",")
    appendLine("        \"lineHighlight\": \"${p.lineHighlight}\",")
    appendLine("        \"inactiveSelection\": \"${p.selection}\"")
    appendLine("      }")
    appendLine("    },")
    val tokens = listOf(
        token("comment", p.comment, italic = true),
        token("keyword, keyword.control, keyword.operator, storage", p.keyword),
        token(
            "storage.type, entity.name.type, entity.name.class, entity.name.struct, support.type",
            p.type
        ),
        token("entity.name.function, entity.name.method, support.function", p.function),
        token("variable, variable.language, variable.parameter", p.variable),
        token("constant, constant.language, constant.numeric, constant.character", p.constant),
        token("constant.character.escape, string.escape", p.escape),
        token("string, string.quoted, support.constant, constant.other", p.string),
        token("punctuation, punctuation.definition, meta.brace", p.punctuation),
        token("punctuation.separator, punctuation.definition.tag", p.separator),
        token("entity.name.tag, meta.tag", p.tag),
        token("entity.other.attribute-name", p.attribute),
        token("invalid, invalid.illegal", p.invalid),
        token("markup.heading", p.function, bold = true),
        token("markup.bold", p.foreground, bold = true),
        token("markup.italic", p.foreground, italic = true),
        token("markup.quote", p.string),
        token("markup.deleted", p.invalid),
        token("markup.inserted", p.keyword),
        token("markup.changed", p.constant)
    )
    tokens.forEachIndexed { index, line ->
        appendLine("    $line${if (index < tokens.lastIndex) "," else ""}")
    }
    appendLine("  ]")
    appendLine("}")
}

private fun token(
    scope: String,
    foreground: String,
    italic: Boolean = false,
    bold: Boolean = false
): String {
    val fontStyle = when {
        bold && italic -> "bold italic"
        bold -> "bold"
        italic -> "italic"
        else -> ""
    }
    val styleSuffix = if (fontStyle.isEmpty()) {
        ""
    } else {
        """, "fontStyle": "$fontStyle""""
    }
    return """{ "scope": "$scope", "settings": { "foreground": "$foreground"$styleSuffix } }"""
}
