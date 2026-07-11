package org.cosmicide.editor.lsp

import org.cosmicide.App
import org.cosmicide.common.Prefs
import org.cosmicide.editor.language.KotlinEditorLanguageProvider
import org.cosmicide.ide.editor.LspServerDefinition
import org.cosmicide.ide.editor.LspServerProvider
import org.cosmicide.ide.editor.LspServerRequest
import org.cosmicide.util.jdksDir
import org.eclipse.lsp4j.CodeLensOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelpOptions
import org.eclipse.lsp4j.jsonrpc.messages.Either

object KotlinLspServerProvider : LspServerProvider {
    override val id = "org.cosmicide.editor.kotlin.intellij"
    override val priority = 200

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension == "kt"
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        val defaultSdk = App.instance.get()
            ?.jdksDir()
            ?.resolve(Prefs.currentJDK)
            ?.takeIf { it.isDirectory }
            ?.absolutePath

        return LspServerDefinition(
            id = id,
            fileExtension = "kt",
            displayName = "Kotlin Language Server",
            connectionFactory = {
                ExistingProcessLspConnection(
                    processProvider = {
                        val context = App.instance.get()
                            ?: throw IllegalStateException("Application context is unavailable")
                        KotlinEditorLanguageProvider.startKotlinLspProcess(
                            context,
                            request.project
                        )
                    }
                )
            },
            grammarScopeName = "source.kotlin",
            initializationOptions = defaultSdk?.let { mapOf("defaultSdk" to it) },
            expectedCapabilities = ServerCapabilities().apply {
                codeActionProvider = Either.forLeft(true)
                documentFormattingProvider = Either.forLeft(true)
                signatureHelpProvider = SignatureHelpOptions(listOf("(", ","))
                diagnosticProvider = null
                definitionProvider = Either.forLeft(true)

                hoverProvider = Either.forLeft(true)
                inlayHintProvider = Either.forLeft(true)
                codeLensProvider = CodeLensOptions(true)
                semanticTokensProvider = null
                documentHighlightProvider = Either.forLeft(false)
            },
            initializationTimeoutMillis = 120_000,
            traceIncomingMessages = true
        )
    }
}
