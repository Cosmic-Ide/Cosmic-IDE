package org.cosmicide.editor.lsp

import android.util.Log
import androidx.compose.material3.ColorScheme
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
    private lateinit var colors: ColorScheme

    fun updateColors(colorScheme: ColorScheme) {
        colors = colorScheme
    }

    override fun supports(request: EditorLanguageRequest): Boolean {
        return request.matchingLspProviders().any()
    }

    override fun configure(request: EditorLanguageRequest): Boolean {
        val lspRequest = LspServerRequest(
            project = request.project,
            file = request.file
        )
        val provider = request.matchingLspProviders().firstOrNull() ?: return false
        LspLogStore.info(
            provider.displayName,
            "Opening ${request.file.name} in ${request.project.name}"
        )
        val definition = runCatching { provider.createDefinition(lspRequest) }
            .onFailure {
                LspLogStore.error(provider.displayName, "Failed to create server definition", it)
            }
            .getOrThrow()
        return request.editor.configureLspLanguage(lspRequest, definition, colors)
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
                    .onFailure {
                        val message = "LSP provider ${provider.id} failed to match"
                        Log.w(TAG, message, it)
                        LspLogStore.warning(TAG, message, it)
                    }
                    .getOrDefault(false)
            }
    }

    private const val TAG = "LspLanguageProvider"
}
