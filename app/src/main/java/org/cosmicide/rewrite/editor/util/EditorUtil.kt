package org.cosmicide.rewrite.editor.util

import androidx.core.content.res.ResourcesCompat
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmicide.rewrite.R

sealed interface EditorLanguage {
    object Kotlin : EditorLanguage
    object Java : EditorLanguage
}

object EditorUtil {

    /*
     * Sets the language of the editor without code completion.
     */
    fun setEditorLanguageTheme(editor: CodeEditor, language: EditorLanguage) {
        when (language) {
            is EditorLanguage.Kotlin -> {
                editor.setEditorLanguage(TextMateLanguage.create("source.kotlin", false))
            }

            is EditorLanguage.Java -> {
                editor.setEditorLanguage(TextMateLanguage.create("source.java", false))
            }
        }
    }

    fun setEditorFont(editor: CodeEditor) {
        editor.typefaceText = ResourcesCompat.getCachedFont(editor.context, R.font.noto_sans_mono)
        editor.isHighlightCurrentLine = true
    }
}