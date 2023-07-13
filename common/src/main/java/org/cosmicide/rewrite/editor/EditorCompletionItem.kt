/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.editor

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

class EditorCompletionItem(
    label: String,
    val description: String,
    prefixLength: Int,
    commitText: String
) : SimpleCompletionItem(label, description, prefixLength, commitText) {

    private val listeners = mutableListOf<(content: Content) -> Unit>()

    override fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        performCompletion(editor, text, position.line, position.column)
    }

    fun setOnComplete(listener: (content: Content) -> Unit) {
        listeners.add(listener)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        if (commitText == null) return

        val suffix = when (kind) {
            CompletionItemKind.Constructor -> "()"
            CompletionItemKind.Module -> "."
            else -> {
                if (kind == CompletionItemKind.Method && commitText.endsWith(')').not()) {
                    "()"
                } else {
                    ""
                }
            }
        }

        text.replace(line, column - prefixLength, line, column, commitText + suffix)
        Log.d("Completion", "commitText: $commitText, suffix: $suffix $label")

        if (kind == CompletionItemKind.Method || kind == CompletionItemKind.Constructor) {
            val start = description.indexOf('(')
            val end = description.indexOf(')')
            if (start == -1) {
                val labelStart = commitText.indexOf('(')
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
        listeners.forEach { it(text) }
    }

    override fun kind(kind: CompletionItemKind?): EditorCompletionItem {
        super.kind(kind)
        return this
    }
}
