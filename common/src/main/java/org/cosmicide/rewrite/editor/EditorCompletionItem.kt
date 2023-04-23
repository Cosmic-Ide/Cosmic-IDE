package org.cosmicide.rewrite.editor

import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

class EditorCompletionItem(
    label: String,
    desc: String,
    prefixLength: Int,
    commitText: String,
) : SimpleCompletionItem(label, desc, prefixLength, commitText) {

    override fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        performCompletion(editor, text, position.line, position.column)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        if (commitText == null) {
            return
        }
        val suffix = when (kind) {
            CompletionItemKind.Method -> "()"
            CompletionItemKind.Constructor -> "()"
            CompletionItemKind.Module -> "."
            else -> {
                ""
            }
        }
        text.replace(line, column - prefixLength, line, column, commitText + suffix)
        if (kind == CompletionItemKind.Method || kind == CompletionItemKind.Constructor) {
            desc.apply {
                if (substring(indexOf('(') + 1, indexOf(')')).isNotEmpty()) {
                    text.cursor.set(text.cursor.leftLine, text.cursor.leftColumn - 1)
                }
            }
        }
    }
}