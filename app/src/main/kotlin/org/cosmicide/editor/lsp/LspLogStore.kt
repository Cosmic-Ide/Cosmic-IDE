package org.cosmicide.editor.lsp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal enum class LspLogLevel(val label: String) {
    DEBUG("D"),
    INFO("I"),
    WARNING("W"),
    ERROR("E")
}

internal data class LspLogEntry(
    val timestampMillis: Long,
    val level: LspLogLevel,
    val source: String,
    val message: String
) {
    fun displayText(): String {
        val timestamp = LOG_TIME_FORMATTER.format(
            Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault())
        )
        return "$timestamp ${level.label}/$source: $message"
    }
}

internal object LspLogStore {
    private val _entries = MutableStateFlow<List<LspLogEntry>>(emptyList())
    val entries: StateFlow<List<LspLogEntry>> = _entries.asStateFlow()

    fun debug(source: String, message: String) = append(LspLogLevel.DEBUG, source, message)

    fun info(source: String, message: String) = append(LspLogLevel.INFO, source, message)

    fun warning(source: String, message: String, throwable: Throwable? = null) =
        append(LspLogLevel.WARNING, source, message, throwable)

    fun error(source: String, message: String, throwable: Throwable? = null) =
        append(LspLogLevel.ERROR, source, message, throwable)

    fun clear() {
        _entries.value = emptyList()
    }

    @Synchronized
    private fun append(
        level: LspLogLevel,
        source: String,
        message: String,
        throwable: Throwable? = null
    ) {
        val fullMessage = if (throwable == null) {
            message
        } else {
            "$message\n${throwable.stackTraceToString()}"
        }
        val entry = LspLogEntry(
            timestampMillis = System.currentTimeMillis(),
            level = level,
            source = source,
            message = fullMessage
        )
        _entries.value = (_entries.value + entry).takeLast(MAX_LOG_ENTRIES)
    }

    private const val MAX_LOG_ENTRIES = 500
}

private val LOG_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
