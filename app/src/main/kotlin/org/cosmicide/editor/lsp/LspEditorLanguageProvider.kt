package org.cosmicide.editor.lsp

import org.cosmicide.ide.editor.EditorExtensionPoints
import org.cosmicide.ide.editor.EditorLanguageProvider
import org.cosmicide.ide.editor.EditorLanguageRequest
import org.cosmicide.ide.editor.LspServerProvider
import org.cosmicide.ide.editor.LspServerRequest
import org.cosmicide.plugin.CosmicPluginHost

object LspEditorLanguageProvider : EditorLanguageProvider {
    override val id = "org.cosmicide.editor.lsp"
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
        return CosmicPluginHost.extensionRegistry
            .extensions(EditorExtensionPoints.LSP_SERVER_PROVIDER)
            .asSequence()
            .filter { it.supports(lspRequest) }
    }
}
