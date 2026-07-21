/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor

import org.cosmicide.plugin.api.ConfigurableExtension
import org.cosmicide.project.Project
import org.eclipse.lsp4j.ServerCapabilities
import java.io.File
import java.io.InputStream
import java.io.OutputStream

data class LspServerRequest(
    val project: Project,
    val file: File
) {
    val extension: String
        get() = file.extension
}

interface LspServerProvider : ConfigurableExtension {
    val priority: Int
        get() = 0

    fun supports(request: LspServerRequest): Boolean

    fun createDefinition(request: LspServerRequest): LspServerDefinition
}

data class LspServerDefinition(
    val id: String,
    val fileExtension: String,
    val displayName: String,
    val connectionFactory: LspServerConnectionFactory,
    val grammarScopeName: String? = null,
    val expectedCapabilities: ServerCapabilities? = null,
    val initializationOptions: Any? = null,
    val configuration: Any? = null,
    val enableInlayHints: Boolean = true,
    val enableSignatureHelp: Boolean = true,
    val initializationTimeoutMillis: Int = 10_000,
    val traceIncomingMessages: Boolean = false,
    val textMateGrammarLink: String? = null
) {
    init {
        require(id.isNotBlank()) { "LSP definition id must not be blank" }
        require(fileExtension.isNotBlank()) { "LSP file extension must not be blank" }
        require(displayName.isNotBlank()) { "LSP display name must not be blank" }
        require(initializationTimeoutMillis > 0) {
            "LSP initialization timeout must be positive"
        }
    }
}

fun interface LspServerConnectionFactory {
    fun create(request: LspServerRequest): LspServerConnection
}

interface LspServerConnection : AutoCloseable {
    fun start()

    val outputStream: OutputStream

    val inputStream: InputStream

    val isClosed: Boolean
}
