/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.editor

import org.cosmicide.plugin.api.Disposable
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.PluginLogger
import org.cosmicide.plugin.api.register
import org.cosmicide.project.CommandRequest
import org.cosmicide.project.IdeServices
import org.cosmicide.project.ToolProcessService
import java.io.InputStream
import java.io.OutputStream

class LspServerBuilder {
    var command: String = ""
    var args: List<String> = emptyList()
    var enableInlayHints: Boolean = true
    var enableSignatureHelp: Boolean = true
    var initializationTimeoutMillis: Int = 120_000
    var priority: Int = 350
    var grammarScopeName: String? = null

    val fileExtensions = mutableSetOf<String>()
    val environment = mutableMapOf<String, String>()
    private val grammarMap = mutableMapOf<String, String>()

    fun fileExtensions(vararg extensions: String) {
        for (ext in extensions) {
            val clean = ext.trim().removePrefix(".")
            if (clean.isNotBlank()) {
                fileExtensions.add(clean.lowercase())
            }
        }
    }

    fun registerGrammar(grammarLink: String, vararg extensions: String) {
        require(grammarLink.isNotBlank()) { "Grammar link must not be blank" }
        for (ext in extensions) {
            val clean = ext.trim().removePrefix(".")
            if (clean.isNotBlank()) {
                val key = if (ext == "M") "M" else clean.lowercase()
                fileExtensions.add(clean.lowercase())
                grammarMap[key] = grammarLink
            }
        }
    }

    fun getGrammarLink(extension: String): String? {
        return grammarMap[extension] ?: grammarMap[extension.lowercase()]
    }
}

/**
 * Type-safe Kotlin builder DSL for registering an LSP server in Cosmic IDE.
 */
fun PluginContext.registerLspServer(
    id: String,
    displayName: String,
    configure: LspServerBuilder.() -> Unit
): Disposable {
    require(id.isNotBlank()) { "LSP id must not be blank" }
    require(displayName.isNotBlank()) { "Display name must not be blank" }

    val builder = LspServerBuilder().apply(configure)
    require(builder.command.isNotBlank()) { "LSP command must not be blank" }
    require(builder.fileExtensions.isNotEmpty()) { "LSP server must register at least one file extension" }

    return registerLspServer(
        id = id,
        displayName = displayName,
        fileExtensions = builder.fileExtensions,
        command = builder.command,
        args = builder.args,
        environmentBuilder = { req ->
            mapOf("COSMIC_PROJECT_ROOT" to req.project.root.absolutePath) + builder.environment
        },
        grammarLinkProvider = { ext -> builder.getGrammarLink(ext) },
        grammarScopeName = builder.grammarScopeName,
        enableInlayHints = builder.enableInlayHints,
        enableSignatureHelp = builder.enableSignatureHelp,
        initializationTimeoutMillis = builder.initializationTimeoutMillis,
        priority = builder.priority
    )
}

/**
 * High-level Kotlin DSL helper for registering a process-backed LSP server in Cosmic IDE.
 * Handles process creation, stream management, stderr logging, and automatic cleanup on plugin unload.
 */
fun PluginContext.registerLspServer(
    id: String,
    displayName: String,
    fileExtensions: Set<String>,
    command: String,
    args: List<String> = emptyList(),
    environmentBuilder: (LspServerRequest) -> Map<String, String> = {
        mapOf("COSMIC_PROJECT_ROOT" to it.project.root.absolutePath)
    },
    grammarLinkProvider: ((extension: String) -> String?)? = null,
    grammarScopeName: String? = null,
    enableInlayHints: Boolean = true,
    enableSignatureHelp: Boolean = true,
    initializationTimeoutMillis: Int = 120_000,
    priority: Int = 350
): Disposable {
    require(id.isNotBlank()) { "LSP id must not be blank" }
    require(displayName.isNotBlank()) { "Display name must not be blank" }
    require(command.isNotBlank()) { "Command must not be blank" }
    require(fileExtensions.isNotEmpty()) { "File extensions must not be empty" }

    val processes = services.require(IdeServices.TOOL_PROCESS)
    val provider = ProcessLspServerProvider(
        id = id,
        displayName = displayName,
        fileExtensions = fileExtensions.map { it.lowercase().removePrefix(".") }.toSet(),
        command = command,
        args = args,
        environmentBuilder = environmentBuilder,
        grammarLinkProvider = grammarLinkProvider,
        grammarScopeName = grammarScopeName,
        enableInlayHints = enableInlayHints,
        enableSignatureHelp = enableSignatureHelp,
        initializationTimeoutMillis = initializationTimeoutMillis,
        priority = priority,
        processes = processes,
        context = this
    )
    return register(EditorExtensionPoints.LSP_SERVER_PROVIDER, provider, priority)
}

fun PluginContext.registerLspServer(
    id: String,
    displayName: String,
    fileExtensions: Set<String>,
    command: String,
    args: List<String> = emptyList(),
    environmentBuilder: (LspServerRequest) -> Map<String, String> = {
        mapOf("COSMIC_PROJECT_ROOT" to it.project.root.absolutePath)
    },
    grammarLink: String? = null,
    grammarScopeName: String? = null,
    enableInlayHints: Boolean = true,
    enableSignatureHelp: Boolean = true,
    initializationTimeoutMillis: Int = 120_000,
    priority: Int = 350
): Disposable = registerLspServer(
    id = id,
    displayName = displayName,
    fileExtensions = fileExtensions,
    command = command,
    args = args,
    environmentBuilder = environmentBuilder,
    grammarLinkProvider = grammarLink?.let { link -> { _: String -> link } },
    grammarScopeName = grammarScopeName,
    enableInlayHints = enableInlayHints,
    enableSignatureHelp = enableSignatureHelp,
    initializationTimeoutMillis = initializationTimeoutMillis,
    priority = priority
)

private class ProcessLspServerProvider(
    override val id: String,
    override val displayName: String,
    private val fileExtensions: Set<String>,
    private val command: String,
    private val args: List<String>,
    private val environmentBuilder: (LspServerRequest) -> Map<String, String>,
    private val grammarLinkProvider: ((extension: String) -> String?)?,
    private val grammarScopeName: String?,
    private val enableInlayHints: Boolean,
    private val enableSignatureHelp: Boolean,
    private val initializationTimeoutMillis: Int,
    override val priority: Int,
    private val processes: ToolProcessService,
    private val context: PluginContext
) : LspServerProvider {

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension.lowercase() in fileExtensions
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        return LspServerDefinition(
            id = id,
            fileExtensions = fileExtensions,
            displayName = displayName,
            connectionFactory = { req ->
                ProcessLspConnection(
                    processes = processes,
                    request = req,
                    command = command,
                    args = args,
                    environment = environmentBuilder(req),
                    logger = context.logger
                )
            },
            grammarScopeName = grammarScopeName,
            textMateGrammarLink = grammarLinkProvider?.invoke(request.extension),
            enableInlayHints = enableInlayHints,
            enableSignatureHelp = enableSignatureHelp,
            initializationTimeoutMillis = initializationTimeoutMillis
        )
    }
}

private class ProcessLspConnection(
    private val processes: ToolProcessService,
    private val request: LspServerRequest,
    private val command: String,
    private val args: List<String>,
    private val environment: Map<String, String>,
    private val logger: PluginLogger
) : LspServerConnection {

    @Volatile
    private var process: Process? = null

    @Synchronized
    override fun start() {
        check(process == null) { "LSP connection '$command' has already started" }
        process = processes.start(
            CommandRequest(
                command = command,
                arguments = args,
                workingDirectory = request.project.root,
                environment = environment
            ),
            redirectErrorStream = false
        ).also { started ->
            drainStderr(started.errorStream)
            logger.info("LSP '$command' started for ${request.project.name}")
        }
    }

    override val outputStream: OutputStream
        get() = checkNotNull(process) { "LSP process has not started" }.outputStream

    override val inputStream: InputStream
        get() = checkNotNull(process) { "LSP process has not started" }.inputStream

    override val isClosed: Boolean
        get() = process?.isAlive != true

    @Synchronized
    override fun close() {
        val running = process ?: return
        process = null
        runCatching { running.outputStream.close() }
        runCatching { running.inputStream.close() }
        runCatching { running.errorStream.close() }
        if (running.isAlive) running.destroy()
    }

    private fun drainStderr(stderr: InputStream) {
        Thread {
            runCatching {
                stderr.bufferedReader().useLines { lines ->
                    lines.forEach(logger::debug)
                }
            }.onFailure {
                logger.warn("LSP '$command' stderr logger stopped", it)
            }
        }.apply {
            name = "$command-LSP-Stderr"
            isDaemon = true
            start()
        }
    }
}
