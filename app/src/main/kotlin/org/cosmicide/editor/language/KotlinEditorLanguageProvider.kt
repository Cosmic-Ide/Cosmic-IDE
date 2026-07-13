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
import org.cosmicide.editor.lsp.ExistingProcessLspConnection
import org.cosmicide.exec.ProcessExecutor
import org.cosmicide.ide.editor.LspServerDefinition
import org.cosmicide.ide.editor.LspServerProvider
import org.cosmicide.ide.editor.LspServerRequest
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
    override val description = "Kotlin editing powered by Kotlin Language Server"
    override val priority = 300

    private var kotlinLspProcess: Process? = null
    private var kotlinProjectRoot: String? = null

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension == "kt"
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        val context = App.instance.get()
            ?: throw IllegalStateException("Application context is unavailable")
        val defaultSdk = context.jdksDir()
            .resolve(Prefs.currentJDK)
            .takeIf { it.isDirectory }
            ?.absolutePath
        return LspServerDefinition(
            id = id,
            fileExtension = "kt",
            displayName = "Kotlin Language Server",
            connectionFactory = {
                ExistingProcessLspConnection {
                    startKotlinLspProcess(context, request.project)
                }
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

    @Synchronized
    internal fun startKotlinLspProcess(context: Context, project: Project): Process? {
        val projectRoot = project.root.absolutePath
        kotlinLspProcess
            ?.takeIf { it.isAlive && kotlinProjectRoot == projectRoot }
            ?.let { return it }

        kotlinLspProcess?.takeIf(Process::isAlive)?.destroy()

        val executable = context.filesDir.resolve("kotlin-lsp/bin/intellij-server")
        val jdkDir = context.jdksDir().resolve(Prefs.currentJDK)
        check(executable.isFile) {
            "Kotlin language server not found at ${executable.absolutePath}"
        }
        check(jdkDir.isDirectory) {
            "Selected JDK not found at ${jdkDir.absolutePath}"
        }
        val gradleJavaHomeOption =
            "-Dcom.jetbrains.ls.imports.gradle.java.home=${jdkDir.absolutePath}"

        val systemPath = context.filesDir.resolve("kotlin-lsp/system").also { it.mkdir() }

        return try {
            ProcessExecutor.startCommand(
                context = context,
                command = executable.absolutePath,
                args = listOf("--stdio", "--system-path", systemPath.absolutePath),
                workingDir = project.root,
                redirectErrorStream = false,
                environmentOverrides = mapOf(
                    "IJ_JAVA_OPTIONS" to gradleJavaHomeOption
                )
            ).also { process ->
                streamStderrToLogcat(process.errorStream)
                kotlinLspProcess = process
                kotlinProjectRoot = projectRoot
                Log.d(TAG, "Kotlin language server started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kotlin language server execution crashed", e)
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
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                Log.d("KOTLIN-LSP", "Stderr logger stopped: ${e.message}")
            }
        }.apply {
            name = "Kotlin-LSP-Stderr-Logger"
            isDaemon = true
            start()
        }
    }

    private const val TAG = "KotlinLanguageProvider"
}
