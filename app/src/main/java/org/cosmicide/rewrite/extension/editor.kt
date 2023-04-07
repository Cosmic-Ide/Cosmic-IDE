package org.cosmicide.rewrite.extension

import android.graphics.drawable.ColorDrawable
import androidx.core.content.res.ResourcesCompat
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.editor.EditorLanguage

/**
 * Sets the language theme for the code editor based on the selected language.
 *
 * @param language The language to set the theme for.
 */
fun CodeEditor.setLanguageTheme(language: EditorLanguage) {
    val textMateLanguage = when (language) {
        EditorLanguage.KOTLIN -> TextMateLanguage.create(EditorLanguage.KOTLIN.source, false)
        EditorLanguage.JAVA -> TextMateLanguage.create(EditorLanguage.JAVA.source, false)
    }
    setEditorLanguage(textMateLanguage)
}

/**
 * Sets the font and enables highlighting of the current line for the code editor.
 */
fun CodeEditor.setFont() {
    typefaceText = ResourcesCompat.getCachedFont(context, R.font.noto_sans_mono)
    isHighlightCurrentLine = true
}