/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.editor.language

import io.github.rosemoe.sora.lang.format.AsyncFormatter
import io.github.rosemoe.sora.langs.textmate.IdeLanguage
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextRange

/**
 * An asynchronous formatter for [IdeLanguage]s.
 *
 * @property language The language to use for formatting.
 */
class IdeFormatter(private val language: IdeLanguage) : AsyncFormatter() {

    override fun formatAsync(text: Content, range: TextRange): TextRange {
        val formattedText = language.formatCode(text)
        text.replace(0, text.length, formattedText)
        return range
    }

    override fun formatRegionAsync(text: Content, range1: TextRange, range2: TextRange): TextRange {
        return range2
    }
}
