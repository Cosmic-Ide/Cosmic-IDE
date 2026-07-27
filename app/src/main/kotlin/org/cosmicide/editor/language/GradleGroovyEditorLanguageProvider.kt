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
import org.cosmicide.project.Project
import java.io.InputStream

object GradleGroovyEditorLanguageProvider : LspServerProvider {
    override val id = "org.cosmicide.editor.gradle-groovy"
    override val displayName = "Gradle/Groovy language support"
    override val description =
        "Gradle and Groovy editing powered by the VS Code Gradle language server"
    override val priority = 300

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension.lowercase() in SUPPORTED_EXTENSIONS
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        val gradleUserHome = runCatching { App.instance.get() }
            .getOrNull()
            ?.filesDir
            ?.resolve("arch/home/.gradle")
            ?.absolutePath
        val settings = mapOf<String, Any?>(
            "gradleHome" to null,
            "gradleVersion" to null,
            "gradleWrapperEnabled" to true,
            "gradleUserHome" to gradleUserHome
        )
        return LspServerDefinition(
            id = id,
            fileExtensions = SUPPORTED_EXTENSIONS,
            displayName = "Gradle Language Server",
            connectionFactory = {
                ExistingProcessLspConnection {
                    val context = App.instance.get()
                        ?: throw IllegalStateException("Application context is unavailable")
                    startServerProcess(context, request.project)
                }
            },
            grammarScopeName = "source.groovy",
            initializationOptions = mapOf("settings" to settings),
            configuration = settings,
            initializationTimeoutMillis = 120_000,
            traceIncomingMessages = true
        )
    }

    internal fun startServerProcess(context: Context, project: Project): Process? {
        val executable = context.filesDir.resolve(SERVER_EXECUTABLE)
        check(executable.isFile) {
            "Gradle language server is not installed at ${executable.absolutePath}"
        }
        val gradleUserHome = context.filesDir.resolve("arch/home/.gradle")

        return try {
            ProcessExecutor.startCommand(
                context = context,
                command = executable.absolutePath,
                workingDir = project.root,
                redirectErrorStream = false,
                environmentOverrides = mapOf(
                    "GRADLE_USER_HOME" to gradleUserHome.absolutePath
                )
            ).also { process ->
                streamStderrToLogcat(process.errorStream)
                Log.d(TAG, "Gradle language server started")
                LspLogStore.info("Gradle Language Server", "Server process started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gradle language server execution crashed", e)
            LspLogStore.error("Gradle Language Server", "Server process crashed", e)
            null
        }
    }

    private fun streamStderrToLogcat(stderr: InputStream) {
        Thread {
            try {
                stderr.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        Log.d("GRADLE-LSP", line)
                        LspLogStore.debug("Gradle Language Server", line)
                    }
                }
            } catch (e: Exception) {
                Log.d("GRADLE-LSP", "Stderr logger stopped: ${e.message}")
                LspLogStore.warning("Gradle Language Server", "Stderr logger stopped", e)
            }
        }.apply {
            name = "Gradle-LSP-Stderr-Logger"
            isDaemon = true
            start()
        }
    }

    private val SUPPORTED_EXTENSIONS = setOf("gradle", "groovy")
    private const val SERVER_EXECUTABLE =
        "vscode-gradle/gradle-language-server/build/install/" +
                "gradle-language-server/bin/gradle-language-server"
    private const val TAG = "GradleLanguageProvider"
}
