/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.language

import android.content.Context
import android.util.Log
import org.cosmicide.App
import org.cosmicide.common.Prefs
import org.cosmicide.editor.LspServerDefinition
import org.cosmicide.editor.LspServerProvider
import org.cosmicide.editor.LspServerRequest
import org.cosmicide.editor.lsp.ExistingProcessLspConnection
import org.cosmicide.editor.lsp.LspLogStore
import org.cosmicide.exec.ProcessExecutor
import org.cosmicide.project.Project
import org.cosmicide.util.jdksDir
import org.eclipse.lsp4j.CodeLensOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelpOptions
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.io.InputStream

object KotlinEditorLanguageProvider : LspServerProvider {
    override val id = "org.cosmicide.editor.kotlin"
    override val displayName = "Kotlin language support"
    override val description = "Kotlin editing powered by fwcd Kotlin Language Server"
    override val priority = 300

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension == "kt"
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        val context = App.instance.get()
            ?: throw IllegalStateException("Application context is unavailable")
        return LspServerDefinition(
            id = id,
            fileExtensions = setOf("kt"),
            displayName = "Kotlin Language Server",
            connectionFactory = {
                ExistingProcessLspConnection {
                    startKotlinLspProcess(context, request.project)
                }
            },
            grammarScopeName = "source.kotlin",
            expectedCapabilities = ServerCapabilities().apply {
                // Code actions power Quick Fixes and Refactorings
                codeActionProvider = Either.forLeft(true)

                // Formatting
                documentFormattingProvider = Either.forLeft(true)
                documentRangeFormattingProvider = Either.forLeft(true)

                // Navigation & Hover
                definitionProvider = Either.forLeft(true)
                implementationProvider = Either.forLeft(true)
                typeDefinitionProvider = Either.forLeft(true)
                referencesProvider = Either.forLeft(true)
                hoverProvider = Either.forLeft(true)
                documentHighlightProvider = Either.forLeft(true)

                // Signatures & Hints
                signatureHelpProvider = SignatureHelpOptions(listOf("(", ","))
                inlayHintProvider = Either.forLeft(true)

                // Code Lens (Method references & implementation counts)
                codeLensProvider = CodeLensOptions(true)


                // NOTE: Standard LSP diagnostic publishing via 'textDocument/publishDiagnostics'
                // does not require setting 'diagnosticProvider' (which is for pull-diagnostics in LSP 3.17+).
                // Setting it to null/omitting it lets standard push diagnostics work.
                diagnosticProvider = null
            },
            enableInlayHints = false,
            initializationTimeoutMillis = 120_000,
            traceIncomingMessages = true
        )
    }

    internal fun startKotlinLspProcess(context: Context, project: Project): Process? {
        val executable =
            context.filesDir.resolve("kotlin-language-server/bin/kotlin-language-server")
        val jdkDir = context.jdksDir().resolve(Prefs.currentJDK)
        check(executable.isFile) {
            "Kotlin language server not found at ${executable.absolutePath}"
        }
        check(jdkDir.isDirectory) {
            "Selected JDK not found at ${jdkDir.absolutePath}"
        }
        return try {
            ProcessExecutor.startCommand(
                context = context,
                command = executable.absolutePath,
                workingDir = project.root,
                redirectErrorStream = false,
                environmentOverrides = mapOf(
                    "JAVA_HOME" to jdkDir.absolutePath
                )
            ).also { process ->
                streamStderrToLogcat(process.errorStream)
                Log.d(TAG, "Kotlin language server started")
                LspLogStore.info("Kotlin Language Server", "Server process started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kotlin language server execution crashed", e)
            LspLogStore.error("Kotlin Language Server", "Server process crashed", e)
            null
        }
    }

    private fun streamStderrToLogcat(stderr: InputStream) {
        Thread {
            try {
                val reader = stderr.bufferedReader()
                var line = reader.readLine()
                while (line != null) {
                    Log.d("KOTLIN-LSP", line)
                    LspLogStore.debug("Kotlin Language Server", line)
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                Log.d("KOTLIN-LSP", "Stderr logger stopped: ${e.message}")
                LspLogStore.warning("Kotlin Language Server", "Stderr logger stopped", e)
            }
        }.apply {
            name = "Kotlin-LSP-Stderr-Logger"
            isDaemon = true
            start()
        }
    }

    private const val TAG = "KotlinLanguageProvider"
}
