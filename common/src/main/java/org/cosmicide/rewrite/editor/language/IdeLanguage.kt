/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rosemoe.sora.langs.textmate

import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.widget.SymbolPairMatch
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.editor.language.IdeFormatter
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.languageconfiguration.model.LanguageConfiguration

/**
 * A language implementation for the IDE.
 *
 * @param grammar The grammar for the language.
 * @param langConfiguration The language configuration.
 * @param grammarRegistry The grammar registry.
 * @param themeRegistry The theme registry.
 * @param createIdentifiers Whether to create identifiers or not.
 */
open class IdeLanguage(
    private val grammar: IGrammar?,
    private val langConfiguration: LanguageConfiguration?,
    private val grammarRegistry: GrammarRegistry,
    private val themeRegistry: ThemeRegistry,
    private val createIdentifiers: Boolean = false
) : TextMateLanguage(
    grammar,
    langConfiguration,
    grammarRegistry,
    themeRegistry,
    createIdentifiers
) {

    private val _symbolPairs: SymbolPairMatch by lazy {
        val symbolPair = SymbolPairMatch()
        langConfiguration?.autoClosingPairs?.forEach { autoClosingPair ->
            symbolPair.putPair(
                autoClosingPair.open,
                SymbolPairMatch.SymbolPair(
                    autoClosingPair.open,
                    autoClosingPair.close,
                    TextMateSymbolPairMatch.SymbolPairEx(autoClosingPair)
                )
            )
        }
        symbolPair
    }

    private val _formatter: IdeFormatter by lazy {
        IdeFormatter(this)
    }

    init {
        tabSize = Prefs.tabSize
        useTab(Prefs.useSpaces.not())
    }

    override fun getFormatter(): Formatter {
        return _formatter
    }

    override fun getSymbolPairs(): SymbolPairMatch {
        return _symbolPairs
    }

    fun formatCode(text: Content): String {
        return text.toString()
    }
}