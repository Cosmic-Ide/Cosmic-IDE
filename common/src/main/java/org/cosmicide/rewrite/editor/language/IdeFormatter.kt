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
        val formattedText = language.formatCode(text, range)
        text.replace(0, text.length, formattedText)
        return range
    }

    override fun formatRegionAsync(text: Content, range1: TextRange, range2: TextRange): TextRange {
        return range2
    }
}