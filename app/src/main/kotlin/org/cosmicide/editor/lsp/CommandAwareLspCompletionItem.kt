/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.lsp

import android.util.Log
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.completion.LspCompletionItem
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.workspace.workSpaceExecuteCommand
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.CompletionItem

/**
 * Adds the LSP-mandated command execution that Sora 0.24.6 omits after accepting a completion.
 */
internal class CommandAwareLspCompletionItem(
    private val completionItem: CompletionItem,
    private val lspEditor: LspEditor,
    prefixLength: Int
) : io.github.rosemoe.sora.lang.completion.CompletionItem(
    completionItem.label,
    completionItem.detail
) {
    private val delegate = LspCompletionItem(
        completionItem,
        lspEditor.eventManager,
        prefixLength
    )

    init {
        label = delegate.label
        detail = delegate.detail
        desc = delegate.desc
        icon = delegate.icon
        deprecated = delegate.deprecated
        kind = delegate.kind
        this.prefixLength = delegate.prefixLength
        sortText = delegate.sortText
        filterText = delegate.filterText
    }

    override fun performCompletion(
        editor: CodeEditor,
        text: Content,
        position: CharPosition
    ) {
        if (!hasEmptyCommandPlaceholderEdit()) {
            delegate.performCompletion(editor, text, position)
        }
        executeCompletionCommand()
    }

    override fun performCompletion(
        editor: CodeEditor,
        text: Content,
        line: Int,
        column: Int
    ) {
        delegate.performCompletion(editor, text, line, column)
        executeCompletionCommand()
    }

    private fun executeCompletionCommand() {
        val command = completionItem.command ?: return
        val commandId = command.command?.takeIf(String::isNotBlank) ?: return
        val arguments = command.arguments ?: emptyList()

        lspEditor.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                lspEditor.eventManager.emitAsync(EventType.workSpaceExecuteCommand) {
                    put("command", commandId)
                    put("args", arguments)
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to execute completion command $commandId", error)
                LspLogStore.warning(
                    "LSP",
                    "Failed to execute completion command $commandId",
                    error
                )
            }
        }
    }

    /**
     * Kotlin LSP command-backed items contain an intentionally empty edit. Applying it locally
     * still causes a full editor change cycle even though the command supplies the real edit.
     */
    private fun hasEmptyCommandPlaceholderEdit(): Boolean {
        if (completionItem.command == null) return false
        val textEdit = completionItem.textEdit ?: return false
        val newText = when {
            textEdit.isLeft -> textEdit.left.newText
            textEdit.isRight -> textEdit.right.newText
            else -> null
        }
        return newText?.isEmpty() == true
    }

    private companion object {
        const val TAG = "LspCompletion"
    }
}
