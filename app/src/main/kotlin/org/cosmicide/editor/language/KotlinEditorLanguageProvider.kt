/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.language

import android.content.Context
import android.util.Log
import org.cosmicide.common.Prefs
import org.cosmicide.editor.lsp.KotlinLspServerProvider
import org.cosmicide.editor.lsp.configureLspLanguage
import org.cosmicide.exec.ProcessExecutor
import org.cosmicide.ide.editor.EditorLanguageProvider
import org.cosmicide.ide.editor.EditorLanguageRequest
import org.cosmicide.ide.editor.LspServerRequest
import org.cosmicide.project.Project
import org.cosmicide.util.jdksDir
import java.io.InputStream

object KotlinEditorLanguageProvider : EditorLanguageProvider {
    override val id = "org.cosmicide.editor.kotlin"
    override val priority = 300

    private var kotlinLspProcess: Process? = null
    private var kotlinProjectRoot: String? = null

    override fun supports(request: EditorLanguageRequest): Boolean {
        return request.file.extension == "kt"
    }

    override fun configure(request: EditorLanguageRequest): Boolean {
        val lspRequest = LspServerRequest(
            project = request.project,
            file = request.file
        )
        val definition = KotlinLspServerProvider.createDefinition(lspRequest)
        return request.editor.configureLspLanguage(lspRequest, definition)
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
