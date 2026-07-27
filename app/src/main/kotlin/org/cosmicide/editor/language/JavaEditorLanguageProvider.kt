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
import org.cosmicide.editor.LspServerDefinition
import org.cosmicide.editor.LspServerProvider
import org.cosmicide.editor.LspServerRequest
import org.cosmicide.editor.lsp.ExistingProcessLspConnection
import org.cosmicide.editor.lsp.LspLogStore
import org.cosmicide.exec.ProcessExecutor
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.project.Project
import org.eclipse.lsp4j.CodeLensOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelpOptions
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.io.File
import java.io.InputStream

object JavaEditorLanguageProvider : LspServerProvider {
    override val id = "org.cosmicide.editor.java"
    override val displayName = "Java language support"
    override val description = "Java editing powered by Eclipse JDT Language Server"
    override val priority = 300

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension == "java"
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        return LspServerDefinition(
            id = id,
            fileExtensions = setOf("java"),
            displayName = "JDT LS",
            connectionFactory = {
                ExistingProcessLspConnection {
                    val context = App.instance.get()
                        ?: throw IllegalStateException("Application context is unavailable")
                    startJdtlsProcess(context, request.project)
                }
            },
            initializationTimeoutMillis = 120_000,
            grammarScopeName = "source.java",
            expectedCapabilities = createJdtCapabilities(),
            configuration = createJdtConfiguration()
        )
    }

    internal fun startJdtlsProcess(context: Context, project: Project): Process? {
        val jdtlsDir = File(context.filesDir, "jdtls")
        val launcherJar = findMainEquinoxLauncher(jdtlsDir)
            ?: throw IllegalStateException("Equinox launcher jar not found in jdtls/plugins")
        val sharedConfigDir = jdtlsDir.resolve("config_linux")
        val workspaceId = "${project.name}-${project.root.absolutePath.hashCode().toUInt()}"

        // Use a writable local configuration while keeping the shared installation read-only.
        val localConfigDir =
            File(context.cacheDir, "jdtls_local_config/$workspaceId").apply { mkdirs() }
        val workspaceDir =
            File(context.cacheDir, "jdtls_workspace/$workspaceId").apply { mkdirs() }

        val command = mutableListOf(
            "-Djdk.xml.maxGeneralEntitySizeLimit=0",
            "-Djdk.xml.totalEntitySizeLimit=0",
            "-Djdk.lang.Process.launchMechanism=FORK",
            "-Declipse.application=org.eclipse.jdt.ls.core.id1",
            "-Dosgi.bundles.defaultStartLevel=4",
            "-Declipse.product=org.eclipse.jdt.ls.core.product",
            "-Djavax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema=org.apache.xenon.BypassFactory",
            "-Dorg.eclipse.xml.disableSchemaValidation=true",
            "-Dlog.level=WARNING",
            "-Xms256m",
            "-Xmx1G",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=50",
            "-XX:+TieredCompilation",
            "-XX:TieredStopAtLevel=1",
            "-Dorg.eclipse.jdt.ls.lombok.support=false",
            "--add-modules=ALL-SYSTEM",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "-Dosgi.checkConfiguration=false",
            "-Dosgi.sharedConfiguration.area=${sharedConfigDir.absolutePath}",
            "-Dosgi.sharedConfiguration.area.readOnly=true",
            "-Dosgi.configuration.cascaded=true",
            "-jar", launcherJar,
            "-configuration", localConfigDir.absolutePath,
            "-data", workspaceDir.absolutePath
        )

        return try {
            ProcessExecutor.startCommand(
                context = context,
                command = "java",
                args = command,
                workingDir = project.root,
                redirectErrorStream = false
            ).also { process ->
                streamStderrToLogcat(process.errorStream)
                val pid = LinuxProcessRunner.getNativePid(process)
                Log.d(
                    TAG,
                    "JDTLS process started with PID: $pid"
                )
                LspLogStore.info("JDT LS", "Server process started with PID: $pid")
                renice(process)
            }
        } catch (e: Exception) {
            Log.e(TAG, "JDTLS execution crashed", e)
            LspLogStore.error("JDT LS", "Server process crashed", e)
            null
        }
    }

    private fun renice(process: Process) {
        try {
            val pid = LinuxProcessRunner.getNativePid(process)
            if (pid != -1) {
                Runtime.getRuntime().exec("renice -n -10 -p $pid")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to renice JDTLS process", e)
        }
    }

    private fun streamStderrToLogcat(stderr: InputStream) {
        Thread {
            try {
                val reader = stderr.bufferedReader()
                var line = reader.readLine()
                while (line != null) {
                    Log.e("JDTLS-LOG", line)
                    LspLogStore.error("JDT LS", line)
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                Log.w("JDTLS-LOG", "Stderr logger stopped: ${e.message}")
                LspLogStore.warning("JDT LS", "Stderr logger stopped", e)
            }
        }.apply {
            name = "JDTLS-Stderr-Logger"
            isDaemon = true
            start()
        }
    }

    private fun findMainEquinoxLauncher(jdtlsDir: File): String? {
        val pluginsDir = File(jdtlsDir, "plugins")
        if (!pluginsDir.isDirectory) return null

        return pluginsDir.listFiles()
            ?.firstOrNull { file ->
                file.name.startsWith("org.eclipse.equinox.launcher_") &&
                        file.name.endsWith(".jar")
            }
            ?.absolutePath
    }

    private fun createJdtCapabilities(): ServerCapabilities {
        return ServerCapabilities().apply {
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
        }
    }

    private fun createJdtConfiguration(): Map<String, Any> {
        return mapOf(
            "settings" to mapOf(
                "java" to mapOf(
                    // Auto-building triggers ongoing diagnostics checks
                    "autobuild" to mapOf("enabled" to true),

                    // Diagnostics and compiler error/warning levels
                    "format" to mapOf("enabled" to true),
                    "saveActions" to mapOf("organizeImports" to true),

                    // Code Lens settings (enable for quick insight links)
                    "implementationsCodeLens" to mapOf("enabled" to true),
                    "referencesCodeLens" to mapOf("enabled" to true),

                    // Quick Fix & Code Actions settings
                    "quickfix" to mapOf(
                        "showInlining" to true
                    ),
                    "codeAction" to mapOf(
                        "sortMembers" to mapOf("enabled" to true)
                    ),

                    // Completion features
                    "completion" to mapOf(
                        "guessMethodArguments" to true,
                        "overwrite" to true,
                        "enabled" to true
                    ),

                    // Code navigation details
                    "references" to mapOf("includeDecompiledSources" to true)
                )
            )
        )
    }

    private const val TAG = "JavaLanguageProvider"
}
