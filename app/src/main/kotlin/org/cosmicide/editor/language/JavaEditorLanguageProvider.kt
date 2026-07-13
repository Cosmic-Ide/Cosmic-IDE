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
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.ide.editor.LspServerDefinition
import org.cosmicide.ide.editor.LspServerProvider
import org.cosmicide.ide.editor.LspServerRequest
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

    private var jdtLspProcess: Process? = null
    private var jdtProjectRoot: String? = null

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension == "java" && Prefs.useJdtLS
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        return LspServerDefinition(
            id = id,
            fileExtension = "java",
            displayName = "JDT LS",
            connectionFactory = {
                ExistingProcessLspConnection {
                    val context = App.instance.get()
                        ?: throw IllegalStateException("Application context is unavailable")
                    startJdtlsProcess(context, request.project)
                }
            },
            grammarScopeName = "source.java",
            expectedCapabilities = createJdtCapabilities(),
            configuration = createJdtConfiguration()
        )
    }

    @Synchronized
    internal fun startJdtlsProcess(context: Context, project: Project): Process? {
        val projectRoot = project.root.absolutePath
        jdtLspProcess
            ?.takeIf { it.isAlive && jdtProjectRoot == projectRoot }
            ?.let { return it }

        jdtLspProcess?.takeIf(Process::isAlive)?.destroy()

        val jdtlsDir = File(context.filesDir, "jdtls")
        val launcherJar = findMainEquinoxLauncher(jdtlsDir)
            ?: throw IllegalStateException("Equinox launcher jar not found in jdtls/plugins")
        val sharedConfigDir = jdtlsDir.resolve("config_linux")

        // Use a writable local configuration while keeping the shared installation read-only.
        val localConfigDir =
            File(context.cacheDir, "jdtls_local_config/${project.name}").apply { mkdirs() }
        val workspaceDir =
            File(context.cacheDir, "jdtls_workspace/${project.name}").apply { mkdirs() }

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
                Log.d(
                    TAG,
                    "JDTLS process started with PID: ${LinuxProcessRunner.getNativePid(process)}"
                )
                renice(process)
                jdtLspProcess = process
                jdtProjectRoot = projectRoot
            }
        } catch (e: Exception) {
            Log.e(TAG, "JDTLS execution crashed", e)
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
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                Log.w("JDTLS-LOG", "Stderr logger stopped: ${e.message}")
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

    private const val TAG = "JavaLanguageProvider"
}
