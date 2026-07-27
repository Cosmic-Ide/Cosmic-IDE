package org.cosmicide.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rosemoe.sora.text.Content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.tooling.RemoteGradleConnector
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import java.io.File

data class EditorToolingState(
    val tasks: List<String> = emptyList(),
    val isSyncing: Boolean = false,
    val error: String? = null
)

class EditorToolingViewModel : ViewModel() {
    var state by mutableStateOf(EditorToolingState())
        private set
    val output = Content().apply {
        setUndoEnabled(false)
    }

    private val pendingOutput = Channel<String>(Channel.UNLIMITED)
    private var connection: ProjectConnection? = null
    private var projectRoot: File? = null
    private var syncJob: Job? = null
    private var syncCancellation: CancellationTokenSource? = null

    init {
        viewModelScope.launch(Dispatchers.Default) { consumeOutput() }
    }

    fun initialize(context: Context, root: File) {
        val absoluteRoot = root.absoluteFile
        if (projectRoot == absoluteRoot && (connection != null || syncJob?.isActive == true)) {
            return
        }
        projectRoot = absoluteRoot
        sync(context.applicationContext, absoluteRoot)
    }

    fun resyncGradle(context: Context) {
        if (syncJob?.isActive == true) return
        projectRoot?.let { sync(context.applicationContext, it) }
    }

    fun stopGradleSync() {
        if (syncJob?.isActive != true) return
        append("Stopping Gradle sync...\n")
        syncCancellation?.cancel()
    }

    private fun sync(context: Context, root: File) {
        val cancellation = GradleConnector.newCancellationTokenSource()
        syncCancellation = cancellation
        syncJob = viewModelScope.launch {
            state = state.copy(isSyncing = true, error = null)
            append("Syncing Gradle project at ${root.absolutePath}\n")
            try {
                val result = withContext(Dispatchers.IO) {
                    RemoteGradleConnector.forProject(context, root)
                        .sync(connection, ::append, cancellation.token())
                }
                connection = result.connection
                state = state.copy(tasks = result.tasks)
                append("Gradle sync finished: ${result.tasks.size} tasks available.\n")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (cancellation.token().isCancellationRequested) {
                    append("Gradle sync cancelled.\n")
                } else {
                    val message = error.message ?: "Unknown Gradle error"
                    state = state.copy(error = message)
                    append("Gradle sync failed: $message\n")
                }
            } finally {
                if (syncCancellation === cancellation) syncCancellation = null
                state = state.copy(isSyncing = false)
            }
        }
    }

    private fun append(text: String) {
        pendingOutput.trySend(text)
    }

    private suspend fun consumeOutput() {
        for (firstChunk in pendingOutput) {
            delay(OUTPUT_BATCH_INTERVAL_MS)
            val batch = Content(firstChunk).apply { setUndoEnabled(false) }
            while (true) {
                batch.appendAtEnd(pendingOutput.tryReceive().getOrNull() ?: break)
            }
            withContext(Dispatchers.Main.immediate) {
                output.appendAtEnd(batch)
                if (output.length > OUTPUT_TRIM_THRESHOLD_CHARS) {
                    output.delete(0, output.length - OUTPUT_RETAINED_CHARS)
                }
            }
        }
    }

    private companion object {
        const val OUTPUT_BATCH_INTERVAL_MS = 50L
        const val OUTPUT_RETAINED_CHARS = 100_000
        const val OUTPUT_TRIM_THRESHOLD_CHARS = 120_000
    }

    override fun onCleared() {
        syncCancellation?.cancel()
        pendingOutput.close()
        connection?.close()
    }
}

private fun Content.appendAtEnd(text: CharSequence) {
    if (text.isEmpty()) return
    val lastLine = lineCount - 1
    insert(lastLine, getColumnCount(lastLine), text)
}
