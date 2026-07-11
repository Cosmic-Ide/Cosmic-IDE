package org.cosmicide.editor.lsp

import android.util.Log
import android.widget.Toast
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.editor.text.MarkdownCodeHighlighterRegistry
import io.github.rosemoe.sora.lsp.editor.text.withEditorHighlighter
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.ide.editor.LspServerConnection
import org.cosmicide.ide.editor.LspServerDefinition
import org.cosmicide.ide.editor.LspServerRequest
import org.eclipse.lsp4j.DidChangeConfigurationParams
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

private const val TAG = "LspEditorAdapter"

fun CodeEditor.configureLspLanguage(
    request: LspServerRequest,
    definition: LspServerDefinition
): Boolean {
    Toast.makeText(
        context,
        "Connecting to ${definition.displayName}...",
        Toast.LENGTH_SHORT
    ).show()
    editable = false

    Timeouts.entries.forEach {
        Timeout[it] = Timeout[it] + 14000
    }

    CoroutineScope(Dispatchers.IO).launch {
        ensureInitializationTimeout(definition.initializationTimeoutMillis)

        val serverDefinition = object : CustomLanguageServerDefinition(
            ext = definition.fileExtension,
            serverConnectProvider = { _ ->
                definition.connectionFactory.create(request).asStreamConnectionProvider(
                    traceIncomingMessages = definition.traceIncomingMessages
                )
            },
            name = definition.displayName,
            expectedCapabilitiesOverride = definition.expectedCapabilities
        ) {
            override fun getInitializationOptions(uri: URI?): Any? {
                return definition.initializationOptions
            }
        }

        val lspProject = LspProject(request.project.root.absolutePath)
        lspProject.addServerDefinition(serverDefinition)

        val lspEditor: LspEditor = lspProject.createEditor(request.file.absolutePath)
        val wrapperLanguage = definition.grammarScopeName?.let {
            createTextMateLanguage(it)
        } ?: EmptyLanguage()

        lspEditor.wrapperLanguage = wrapperLanguage
        lspEditor.isEnableInlayHint = definition.enableInlayHints
        lspEditor.isEnableSignatureHelp = definition.enableSignatureHelp

        withContext(Dispatchers.Main) {
            lspEditor.editor = this@configureLspLanguage
        }

        try {
            lspEditor.connectWithTimeout()
            this@configureLspLanguage.editable = true

            definition.configuration?.let {
                lspEditor.requestManager.didChangeConfiguration(
                    DidChangeConfigurationParams(it)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to connect to ${definition.displayName}", e)
        }
    }

    return true
}

@Synchronized
private fun ensureInitializationTimeout(timeoutMillis: Int) {
    if (Timeout[Timeouts.INIT] < timeoutMillis) {
        Timeout[Timeouts.INIT] = timeoutMillis
    }
}

private fun LspServerConnection.asStreamConnectionProvider(
    traceIncomingMessages: Boolean
): StreamConnectionProvider {
    return object : StreamConnectionProvider {
        private val serverInputStream: InputStream by lazy {
            this@asStreamConnectionProvider.inputStream.let { input ->
                if (traceIncomingMessages) {
                    LspMessageTracingInputStream(input, "KOTLIN-LSP-IN")
                } else {
                    input
                }
            }
        }

        override fun start() {
            this@asStreamConnectionProvider.start()
        }

        override val outputStream: OutputStream
            get() = this@asStreamConnectionProvider.outputStream

        override val inputStream: InputStream
            get() = serverInputStream

        override val isClosed: Boolean
            get() = this@asStreamConnectionProvider.isClosed

        override fun close() {
            this@asStreamConnectionProvider.close()
        }
    }
}

private class LspMessageTracingInputStream(
    input: InputStream,
    private val logTag: String
) : FilterInputStream(input) {

    private val pending = ByteArrayOutputStream()

    override fun read(): Int {
        return super.read().also { value ->
            if (value >= 0) trace(byteArrayOf(value.toByte()), 0, 1)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return super.read(buffer, offset, length).also { count ->
            if (count > 0) trace(buffer, offset, count)
        }
    }

    @Synchronized
    private fun trace(buffer: ByteArray, offset: Int, length: Int) {
        pending.write(buffer, offset, length)

        while (true) {
            val bytes = pending.toByteArray()
            val headerEnd = bytes.indexOfHeaderEnd()
            if (headerEnd < 0) return

            val headers = String(bytes, 0, headerEnd, Charsets.US_ASCII)
            val contentLength = headers.lineSequence()
                .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                ?.toIntOrNull()

            if (contentLength == null) {
                Log.w(logTag, "Unable to trace malformed LSP frame headers: $headers")
                pending.reset()
                return
            }

            val bodyStart = headerEnd + HEADER_TERMINATOR.size
            val frameEnd = bodyStart + contentLength
            if (bytes.size < frameEnd) return

            logMessage(String(bytes, bodyStart, contentLength, Charsets.UTF_8))
            pending.reset()
            if (frameEnd < bytes.size) {
                pending.write(bytes, frameEnd, bytes.size - frameEnd)
            }
        }
    }

    private fun logMessage(message: String) {
        message.chunked(LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
            Log.d(logTag, "<-- [$index] $chunk")
        }
    }

    private fun ByteArray.indexOfHeaderEnd(): Int {
        if (size < HEADER_TERMINATOR.size) return -1
        for (index in 0..size - HEADER_TERMINATOR.size) {
            if (HEADER_TERMINATOR.indices.all { offset ->
                    this[index + offset] == HEADER_TERMINATOR[offset]
                }) {
                return index
            }
        }
        return -1
    }

    private companion object {
        private val HEADER_TERMINATOR = "\r\n\r\n".toByteArray(Charsets.US_ASCII)
        private const val LOG_CHUNK_SIZE = 3_500
    }
}

class ExistingProcessLspConnection(
    private val processProvider: () -> Process?
) : LspServerConnection {

    private var process: Process? = null
    private var closed = false

    override fun start() {
        process = processProvider()
            ?: throw IllegalStateException("Language server process is not running")
        closed = false
    }

    override val outputStream: OutputStream
        get() = process?.outputStream
            ?: throw IllegalStateException("Language server process not started")

    override val inputStream: InputStream
        get() = process?.inputStream
            ?: throw IllegalStateException("Language server process not started")

    override val isClosed: Boolean
        get() = closed

    override fun close() {
        if (!closed) {
            process?.destroy()
            closed = true
        }
    }
}

private fun createTextMateLanguage(grammarScopeName: String): TextMateLanguage {
    MarkdownCodeHighlighterRegistry.global.withEditorHighlighter {
        Pair(
            TextMateLanguage.create(grammarScopeName, false),
            TextMateColorScheme.create(ThemeRegistry.getInstance())
        )
    }

    return TextMateLanguage.create(
        grammarScopeName, false
    )
}
