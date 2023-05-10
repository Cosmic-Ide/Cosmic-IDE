package org.cosmicide.rewrite.editor

import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

class EditorCompletionItem(
    val label: String,
    val description: String,
    val prefixLength: Int,
    val commitText: String
) : SimpleCompletionItem(label, description, prefixLength, commitText) {

    override fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        performCompletion(editor, text, position.line, position.column)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        if (commitText == null) return

        val suffix = when (kind) {
            CompletionItemKind.Method, CompletionItemKind.Constructor -> "()"
            CompletionItemKind.Module -> "."
            else -> ""
        }

        text.replace(line, column - prefixLength, line, column, commitText + suffix)

        if (kind == CompletionItemKind.Method || kind == CompletionItemKind.Constructor) {
            val start = description.indexOf('(')
            val end = description.indexOf(')')
            if (start == -1) {
                val labelStart = label.indexOf('(')
                val labelEnd = label.indexOf(')')
                if (labelStart == -1) return
                if (label.substring(labelStart + 1, labelEnd).isNotEmpty()) {
                    text.cursor.set(text.cursor.leftLine, text.cursor.leftColumn - 1)
                }
            } else {
                if (description.substring(start + 1, end).isNotEmpty()) {
                    text.cursor.set(text.cursor.leftLine, text.cursor.leftColumn - 1)
                }
            }
        }
    }
}