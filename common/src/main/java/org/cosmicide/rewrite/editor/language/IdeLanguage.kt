package io.github.rosemoe.sora.langs.textmate

import android.os.Bundle
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.widget.SymbolPairMatch
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.editor.language.IdeFormatter
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.languageconfiguration.model.AutoClosingPairConditional
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
) : TextMateLanguage(grammar, langConfiguration, grammarRegistry, themeRegistry, createIdentifiers) {

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

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {}

    override fun getFormatter(): Formatter {
        return _formatter
    }

    override fun getSymbolPairs(): SymbolPairMatch {
        return _symbolPairs
    }

    fun formatCode(text: Content, range: TextRange): String {
        return text.toString()
    }
}