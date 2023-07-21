/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.editor.theme

import io.github.rosemoe.sora.editor.ts.TsThemeBuilder
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

object ThemeDarculaBuilder {
    fun applyTo(builder: TsThemeBuilder) {
        builder.apply {
            TextStyle.makeStyle(EditorColorScheme.COMMENT, 0, false, true, false) applyTo "comment"
            TextStyle.makeStyle(EditorColorScheme.KEYWORD, 0, true, false, false) applyTo "keyword"
            TextStyle.makeStyle(EditorColorScheme.LITERAL) applyTo arrayOf(
                "constant.builtin",
                "string",
                "number"
            )
            TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_VAR) applyTo arrayOf(
                "variable.builtin",
                "variable",
                "constant"
            )
            TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_NAME) applyTo arrayOf(
                "type.builtin",
                "type",
                "attribute"
            )
            TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME) applyTo arrayOf(
                "function.method",
                "function.builtin",
                "variable.field"
            )
            TextStyle.makeStyle(EditorColorScheme.OPERATOR) applyTo arrayOf("operator")
        }
    }
}