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
import org.cosmicide.editor.lsp.ExistingProcessLspConnection
import org.cosmicide.exec.ProcessExecutor
import org.cosmicide.ide.editor.LspServerDefinition
import org.cosmicide.ide.editor.LspServerProvider
import org.cosmicide.ide.editor.LspServerRequest
import org.cosmicide.project.Project
import java.io.InputStream

object ScalaEditorLanguageProvider : LspServerProvider {
    override val id = "org.cosmicide.editor.scala"
    override val displayName = "Scala language support"
    override val description = "Scala editing powered by Metals"
    override val priority = 300

    private var metalsProcess: Process? = null
    private var metalsProjectRoot: String? = null

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension in SCALA_EXTENSIONS
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        return LspServerDefinition(
            id = id,
            fileExtension = request.extension,
            displayName = "Metals",
            connectionFactory = {
                ExistingProcessLspConnection {
                    val context = App.instance.get()
                        ?: throw IllegalStateException("Application context is unavailable")
                    startMetalsProcess(context, request.project)
                }
            },
            grammarScopeName = "source.scala",
            initializationTimeoutMillis = 120_000
        )
    }

    @Synchronized
    internal fun startMetalsProcess(context: Context, project: Project): Process? {
        val projectRoot = project.root.absolutePath
        metalsProcess
            ?.takeIf { it.isAlive && metalsProjectRoot == projectRoot }
            ?.let { return it }

        metalsProcess?.takeIf(Process::isAlive)?.destroy()

        val executable = context.filesDir.resolve("scala/bin/metals")
        check(executable.isFile) {
            "Metals is not installed at ${executable.absolutePath}"
        }

        return try {
            ProcessExecutor.startCommand(
                context = context,
                command = executable.absolutePath,
                workingDir = project.root,
                redirectErrorStream = false
            ).also { process ->
                streamStderrToLogcat(process.errorStream)
                metalsProcess = process
                metalsProjectRoot = projectRoot
                Log.d(TAG, "Metals language server started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Metals execution crashed", e)
            null
        }
    }

    private fun streamStderrToLogcat(stderr: InputStream) {
        Thread {
            try {
                val reader = stderr.bufferedReader()
                var line = reader.readLine()
                while (line != null) {
                    Log.d("METALS-LSP", line)
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                Log.d("METALS-LSP", "Stderr logger stopped: ${e.message}")
            }
        }.apply {
            name = "Metals-LSP-Stderr-Logger"
            isDaemon = true
            start()
        }
    }

    internal val supportedExtensions: Set<String>
        get() = SCALA_EXTENSIONS

    private val SCALA_EXTENSIONS = setOf("scala", "sc", "sbt")
    private const val TAG = "ScalaLanguageProvider"
}
