package org.cosmicide.editor.lsp

import android.util.Log
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.EditorLanguageProvider
import org.cosmicide.editor.EditorLanguageRequest
import org.cosmicide.editor.LspServerProvider
import org.cosmicide.editor.LspServerRequest
import org.cosmicide.plugin.CosmicPluginHost

object LspEditorLanguageProvider : EditorLanguageProvider {
    override val id = "org.cosmicide.editor.lsp"
    override val displayName = "Language Server Protocol editor"
    override val description = "Connects editor documents to registered language servers"
    override val canDisable = false
    override val priority = 200

    override fun supports(request: EditorLanguageRequest): Boolean {
        return request.matchingLspProviders().any()
    }

    override fun configure(request: EditorLanguageRequest): Boolean {
        val lspRequest = LspServerRequest(
            project = request.project,
            file = request.file
        )
        val provider = request.matchingLspProviders().firstOrNull() ?: return false
        val definition = provider.createDefinition(lspRequest)
        return request.editor.configureLspLanguage(lspRequest, definition)
    }

    private fun EditorLanguageRequest.matchingLspProviders(): Sequence<LspServerProvider> {
        val lspRequest = LspServerRequest(
            project = project,
            file = file
        )
        return CosmicPluginHost
            .enabledExtensions(EditorExtensionPoints.LSP_SERVER_PROVIDER)
            .asSequence()
            .filter { provider ->
                runCatching { provider.supports(lspRequest) }
                    .onFailure { Log.w(TAG, "LSP provider ${provider.id} failed to match", it) }
                    .getOrDefault(false)
            }
    }

    private const val TAG = "LspLanguageProvider"
}
