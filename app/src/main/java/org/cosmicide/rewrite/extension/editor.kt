package org.cosmicide.rewrite.extension

import androidx.core.content.res.ResourcesCompat
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.editor.completion.CustomCompletionItemAdapter
import org.cosmicide.rewrite.editor.completion.CustomCompletionLayout

/**
 * Sets the font and enables highlighting of the current line for the code editor.
 */
fun CodeEditor.setFont() {
    typefaceText = ResourcesCompat.getFont(context, R.font.noto_sans_mono)
    isHighlightCurrentLine = true
}

fun CodeEditor.setCompletionLayout() {
    getComponent(EditorAutoCompletion::class.java).apply {
        setAdapter(CustomCompletionItemAdapter())
        setLayout(CustomCompletionLayout())
    }
}