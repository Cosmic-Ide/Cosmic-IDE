package org.cosmicide.rewrite.editor.language

import io.github.rosemoe.sora.lang.format.AsyncFormatter
import io.github.rosemoe.sora.langs.textmate.IdeLanguage
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextRange

class IdeFormatter(val language: IdeLanguage) : AsyncFormatter() {

    override fun formatAsync(text: Content, range: TextRange): TextRange {
        text.replace(0, text.toString().length, language.formatCode(text, range))
        return range
    }

    override fun formatRegionAsync(text: Content, range1: TextRange, range2: TextRange): TextRange {
        return range2
    }
}