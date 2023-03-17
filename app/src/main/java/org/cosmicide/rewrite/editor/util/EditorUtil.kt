package org.cosmicide.rewrite.editor.util

import android.graphics.Typeface
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor

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
        editor.typefaceText =
            Typeface.createFromAsset(editor.context.assets, "fonts/JetBrainsMono-Light.ttf")
    }
}