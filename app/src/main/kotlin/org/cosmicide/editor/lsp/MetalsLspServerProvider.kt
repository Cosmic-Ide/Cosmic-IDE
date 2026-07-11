package org.cosmicide.editor.lsp

import org.cosmicide.App
import org.cosmicide.editor.language.ScalaEditorLanguageProvider
import org.cosmicide.ide.editor.LspServerDefinition
import org.cosmicide.ide.editor.LspServerProvider
import org.cosmicide.ide.editor.LspServerRequest

object MetalsLspServerProvider : LspServerProvider {
    override val id = "org.cosmicide.editor.scala.metals"
    override val priority = 200

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension in ScalaEditorLanguageProvider.supportedExtensions
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        return LspServerDefinition(
            id = id,
            fileExtension = request.extension,
            displayName = "Metals",
            connectionFactory = {
                ExistingProcessLspConnection(
                    processProvider = {
                        val context = App.instance.get()
                            ?: throw IllegalStateException("Application context is unavailable")
                        ScalaEditorLanguageProvider.startMetalsProcess(context, request.project)
                    }
                )
            },
            grammarScopeName = "source.scala",
            initializationTimeoutMillis = 120_000
        )
    }
}
