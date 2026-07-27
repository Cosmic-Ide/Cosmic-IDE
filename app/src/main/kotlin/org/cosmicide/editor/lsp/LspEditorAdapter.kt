package org.cosmicide.editor.lsp

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.ColorScheme
import androidx.core.net.toUri
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.editor.text.MarkdownCodeHighlighterRegistry
import io.github.rosemoe.sora.lsp.editor.text.withEditorHighlighter
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.getComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.editor.LspServerConnection
import org.cosmicide.editor.LspServerDefinition
import org.cosmicide.editor.LspServerRequest
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.tm4e.core.registry.IGrammarSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "LspEditorAdapter"

fun CodeEditor.configureLspLanguage(
    request: LspServerRequest,
    definition: LspServerDefinition,
    colorScheme: ColorScheme
): Boolean {
    LspLogStore.info(definition.displayName, "Connecting to language server")
    editable = false

    val lspProject = LspProjects.forRoot(request.project.root.absolutePath)
    Timeouts.entries.forEach {
        Timeout[it] = Timeout[it] + 14000
    }

    CoroutineScope(Dispatchers.IO).launch {
        ensureInitializationTimeout(definition.initializationTimeoutMillis)

        val serverDefinition = object : CustomLanguageServerDefinition(
            ext = definition.fileExtension,
            serverConnectProvider = { _ ->
                definition.connectionFactory.create(request).asStreamConnectionProvider(
                    traceIncomingMessages = definition.traceIncomingMessages,
                    logSource = definition.displayName
                )
            },
            name = definition.displayName,
            expectedCapabilitiesOverride = definition.expectedCapabilities
        ) {
            override fun getInitializationOptions(uri: URI?): Any? {
                return definition.initializationOptions
            }
        }

        synchronized(lspProject) {
            if (
                lspProject.getServerDefinition(
                    serverDefinition.ext,
                    serverDefinition.name
                ) == null
            ) {
                lspProject.addServerDefinition(serverDefinition)
            }
        }

        val previousEditors = lspProject.getEditors()
            .filter { it.editor === this@configureLspLanguage }
        val lspEditor = lspProject.createEditor(request.file.absolutePath)
        var grammarFailure: Throwable? = null
        val wrapperLanguage = runCatching {
            createTextMateLanguage(context, definition)
        }.onFailure {
            grammarFailure = it
            Log.w(TAG, "Failed to load TextMate grammar for ${definition.displayName}", it)
            LspLogStore.warning(
                definition.displayName,
                "Failed to load TextMate grammar",
                it
            )
        }.getOrElse {
            definition.grammarScopeName?.let { scopeName ->
                runCatching { createTextMateLanguage(scopeName) }.getOrNull()
            } ?: EmptyLanguage()
        }

        lspEditor.wrapperLanguage = wrapperLanguage
        lspEditor.isEnableInlayHint = definition.enableInlayHints
        lspEditor.isEnableSignatureHelp = definition.enableSignatureHelp

        withContext(Dispatchers.Main) {
            lspEditor.editor = this@configureLspLanguage
            grammarFailure?.let {
                Toast.makeText(
                    context,
                    "Could not load the TextMate grammar: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        try {
            lspEditor.connectWithTimeout()
            withContext(Dispatchers.Main) {
                editable = true
                lspEditor.hoverWindow?.layout = HoverLayout()
                lspEditor.signatureHelpWindow?.layout =
                    ComposeSignatureHelpLayout()
                getComponent<EditorDiagnosticTooltipWindow>().layout =
                    LspDiagnosticTooltipLayout(lspEditor, colorScheme)
            }
            previousEditors.forEach(LspEditor::dispose)
            LspLogStore.info(definition.displayName, "Language server connected")

            definition.configuration?.let {
                lspEditor.requestManager.didChangeConfiguration(
                    DidChangeConfigurationParams(it)
                )
            }
        } catch (e: Exception) {
            lspEditor.dispose()
            Log.w(TAG, "Failed to connect to ${definition.displayName}", e)
            LspLogStore.error(definition.displayName, "Failed to connect", e)
        }
    }

    return true
}

fun CodeEditor.disposeLspLanguage() {
    val lspEditors = LspProjects.editorsFor(this)
    CoroutineScope(Dispatchers.IO).launch {
        lspEditors.forEach(LspEditor::dispose)
    }
}

private object LspProjects {
    private val projects = ConcurrentHashMap<String, LspProject>()

    fun forRoot(projectRoot: String): LspProject {
        return projects.computeIfAbsent(projectRoot, ::LspProject)
    }

    fun editorsFor(editor: CodeEditor): List<LspEditor> {
        return projects.values.flatMap(LspProject::getEditors)
            .filter { it.editor === editor }
    }
}

@Synchronized
private fun ensureInitializationTimeout(timeoutMillis: Int) {
    if (Timeout[Timeouts.INIT] < timeoutMillis) {
        Timeout[Timeouts.INIT] = timeoutMillis
    }
}

private fun LspServerConnection.asStreamConnectionProvider(
    traceIncomingMessages: Boolean,
    logSource: String
): StreamConnectionProvider {
    return object : StreamConnectionProvider {
        private val serverInputStream: InputStream by lazy {
            this@asStreamConnectionProvider.inputStream.let { input ->
                if (traceIncomingMessages) {
                    LspMessageTracingInputStream(input, "KOTLIN-LSP-IN", logSource)
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
    private val logTag: String,
    private val logSource: String
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
                LspLogStore.warning(
                    logSource,
                    "Unable to trace malformed LSP frame headers: $headers"
                )
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
            LspLogStore.debug(logSource, "<-- [$index] $chunk")
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

@Synchronized
private fun createTextMateLanguage(
    context: Context,
    definition: LspServerDefinition
): io.github.rosemoe.sora.lang.Language {
    val grammarLink = definition.textMateGrammarLink
        ?: return definition.grammarScopeName?.let(::createTextMateLanguage) ?: EmptyLanguage()
    if (!grammarLink.startsWith("https://", ignoreCase = true)) {
        val grammarText = openGrammarStream(context, grammarLink).readGrammarText()
        return createTextMateLanguage(definition, grammarText)
    }

    val cacheFile = grammarCacheFile(context, grammarLink)
    var cachedGrammarText: String? = null
    if (cacheFile.isFile) {
        cachedGrammarText = runCatching { cacheFile.inputStream().readGrammarText() }
            .onFailure {
                Log.w(TAG, "Discarding unreadable grammar cache ${cacheFile.name}", it)
                cacheFile.delete()
            }
            .getOrNull()
    }

    if (cachedGrammarText != null && isTextMateGrammarCacheFresh(cacheFile)) {
        try {
            return createTextMateLanguage(definition, cachedGrammarText)
        } catch (e: Exception) {
            Log.w(TAG, "Discarding invalid grammar cache ${cacheFile.name}", e)
            cacheFile.delete()
            cachedGrammarText = null
        }
    }

    val refreshedGrammarText = try {
        openGrammarStream(context, grammarLink).readGrammarText()
    } catch (refreshFailure: Exception) {
        val staleGrammarText = cachedGrammarText ?: throw refreshFailure
        Log.w(TAG, "Grammar refresh failed; using stale cache for $grammarLink", refreshFailure)
        return createTextMateLanguage(definition, staleGrammarText)
    }

    return try {
        createTextMateLanguage(definition, refreshedGrammarText).also {
            runCatching { cacheGrammar(cacheFile, refreshedGrammarText) }
                .onFailure { error ->
                    Log.w(TAG, "Unable to cache grammar from $grammarLink", error)
                }
        }
    } catch (refreshFailure: Exception) {
        val staleGrammarText = cachedGrammarText ?: throw refreshFailure
        Log.w(
            TAG,
            "Refreshed grammar is invalid; using stale cache for $grammarLink",
            refreshFailure
        )
        runCatching { createTextMateLanguage(definition, staleGrammarText) }
            .getOrElse { staleFailure ->
                refreshFailure.addSuppressed(staleFailure)
                throw refreshFailure
            }
    }
}

private fun createTextMateLanguage(
    definition: LspServerDefinition,
    rawGrammarText: String
): TextMateLanguage {
    val grammarText = rawGrammarText.removePrefix("\uFEFF")
    require(grammarText.isNotBlank()) { "Grammar file is empty" }
    val contentType = when (grammarText.firstOrNull { !it.isWhitespace() }) {
        '{', '[' -> IGrammarSource.ContentType.JSON
        '<' -> IGrammarSource.ContentType.XML
        else -> IGrammarSource.ContentType.YAML
    }

    fun createLanguage(): TextMateLanguage {
        val grammarSource = IGrammarSource.fromString(contentType, grammarText)
        val grammarDefinition = DefaultGrammarDefinition.withGrammarSource(
            grammarSource,
            definition.id,
            null
        )
        val themeRegistry = ThemeRegistry.getInstance()
        val grammarRegistry = GrammarRegistry(GrammarRegistry.getInstance()).apply {
            setTheme(themeRegistry.currentThemeModel)
        }
        return TextMateLanguage.create(
            grammarDefinition,
            grammarRegistry,
            themeRegistry,
            false
        )
    }

    val language = createLanguage()
    MarkdownCodeHighlighterRegistry.global.withEditorHighlighter {
        Pair(
            createLanguage(),
            TextMateColorScheme.create(ThemeRegistry.getInstance())
        )
    }
    return language
}

private fun grammarCacheFile(context: Context, grammarLink: String): File {
    val cacheKey = MessageDigest.getInstance("SHA-256")
        .digest(grammarLink.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    return context.cacheDir
        .resolve(GRAMMAR_CACHE_DIRECTORY)
        .also(File::mkdirs)
        .resolve("$cacheKey.grammar")
}

private fun cacheGrammar(cacheFile: File, grammarText: String) {
    val temporaryFile = File.createTempFile(cacheFile.name, ".tmp", cacheFile.parentFile)
    try {
        temporaryFile.writeText(grammarText, Charsets.UTF_8)
        try {
            Files.move(
                temporaryFile.toPath(),
                cacheFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporaryFile.toPath(),
                cacheFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
        cacheFile.setLastModified(System.currentTimeMillis())
    } finally {
        temporaryFile.delete()
    }
}

private fun openGrammarStream(context: Context, link: String): InputStream {
    val uri = link.toUri()
    return when (uri.scheme?.lowercase()) {
        "http", "https" -> URL(link).openConnection().apply {
            connectTimeout = GRAMMAR_CONNECT_TIMEOUT_MILLIS
            readTimeout = GRAMMAR_READ_TIMEOUT_MILLIS
        }.getInputStream()

        "content" -> context.contentResolver.openInputStream(uri)
            ?: error("Unable to open grammar content URI")

        "file" -> File(uri.path ?: error("Grammar file path is missing")).inputStream()
        null -> File(link).inputStream()
        else -> error("Unsupported grammar link: ${uri.scheme}")
    }
}

private fun InputStream.readGrammarText(): String = use { input ->
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        totalBytes += count
        require(totalBytes <= MAX_GRAMMAR_BYTES) {
            "Grammar file is larger than ${MAX_GRAMMAR_BYTES / (1024 * 1024)} MB"
        }
        output.write(buffer, 0, count)
    }
    output.toString(Charsets.UTF_8.name())
}

private const val MAX_GRAMMAR_BYTES = 5 * 1024 * 1024
private const val GRAMMAR_CONNECT_TIMEOUT_MILLIS = 15_000
private const val GRAMMAR_READ_TIMEOUT_MILLIS = 30_000
private const val GRAMMAR_CACHE_DIRECTORY = "textmate-grammar-cache"
