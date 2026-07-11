package org.cosmicide.editor.lsp

import org.cosmicide.App
import org.cosmicide.common.Prefs
import org.cosmicide.editor.language.JavaEditorLanguageProvider
import org.cosmicide.ide.editor.LspServerDefinition
import org.cosmicide.ide.editor.LspServerProvider
import org.cosmicide.ide.editor.LspServerRequest
import org.eclipse.lsp4j.CodeLensOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelpOptions
import org.eclipse.lsp4j.jsonrpc.messages.Either

object JdtLspServerProvider : LspServerProvider {
    override val id = "org.cosmicide.editor.java.jdt"
    override val priority = 200

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension == "java" && Prefs.useJdtLS
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        return LspServerDefinition(
            id = id,
            fileExtension = "java",
            displayName = "JDT LS",
            connectionFactory = {
                ExistingProcessLspConnection(
                    processProvider = {
                        val context = App.instance.get()
                            ?: throw IllegalStateException("Application context is unavailable")
                        JavaEditorLanguageProvider.startJdtlsProcess(context, request.project)
                    }
                )
            },
            grammarScopeName = "source.java",
            expectedCapabilities = createJdtCapabilities(),
            configuration = createJdtConfiguration()
        )
    }

    private fun createJdtCapabilities(): ServerCapabilities {
        return ServerCapabilities().apply {
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
        }
    }

    private fun createJdtConfiguration(): Map<String, Any> {
        return mapOf(
            "settings" to mapOf(
                "java" to mapOf(
                    "autobuild" to mapOf("enabled" to false),
                    "references" to mapOf("includeDecompiledSources" to false),
                    "completion" to mapOf(
                        "guessMethodArguments" to false,
                        "favoriteStaticMembers" to emptyList<String>()
                    ),
                    "implementationsCodeLens" to mapOf("enabled" to false),
                    "referencesCodeLens" to mapOf("enabled" to false)
                )
            )
        )
    }
}
