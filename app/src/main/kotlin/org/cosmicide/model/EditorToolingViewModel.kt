package org.cosmicide.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.tooling.RemoteGradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.model.gradle.BuildInvocations
import java.io.File
import java.io.OutputStream

class EditorToolingViewModel : ViewModel() {
    var connection: ProjectConnection? by mutableStateOf(null)
        private set

    var tasks by mutableStateOf<List<String>>(emptyList())
        private set

    var isSyncing by mutableStateOf(false)
        private set

    var syncError by mutableStateOf<String?>(null)
        private set

    var output by mutableStateOf("")
        private set

    private val pendingOutput = Channel<String>(Channel.UNLIMITED)
    private var projectRoot: File? = null
    private var syncJob: Job? = null
    private var tasksInitialized = false

    init {
        viewModelScope.launch {
            for (text in pendingOutput) {
                val combined = output + text
                output = if (combined.length > MAX_OUTPUT_CHARS) {
                    combined.takeLast(MAX_OUTPUT_CHARS)
                } else {
                    combined
                }
            }
        }
    }

    fun initialize(context: Context, root: File) {
        val absoluteRoot = root.absoluteFile
        if (projectRoot == absoluteRoot && (tasksInitialized || connection != null || syncJob?.isActive == true)) {
            return
        }

        projectRoot = absoluteRoot
        startSync(context.applicationContext)
    }

    fun resyncGradle(context: Context) {
        if (syncJob?.isActive == true) return
        startSync(context.applicationContext)
    }

    private fun startSync(context: Context) {
        val root = projectRoot ?: return
        syncJob = viewModelScope.launch {
            isSyncing = true
            syncError = null
            enqueueOutput("Syncing Gradle project at ${root.absolutePath}\n")

            val currentConnection = connection
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val activeConnection = currentConnection
                        ?: RemoteGradleConnector(context).forProjectDirectory(root).connect()

                    val stream = ToolingLogOutputStream(::enqueueOutput)
                    val model = activeConnection.model(BuildInvocations::class.java)
                        .setStandardOutput(stream).setStandardError(stream)
                        .addProgressListener(
                            {
                                enqueueOutput("${it.displayName}\n")
                            },
                            OperationType.BUILD_PHASE,
                            OperationType.FILE_DOWNLOAD,
                            OperationType.TASK,
                            OperationType.PROBLEMS
                        ).get()

                    val loadedTasks =
                        (model.tasks.map { it.path } + model.taskSelectors.map { it.name }).distinct()
                            .sorted()

                    activeConnection to loadedTasks
                }
            }

            result.onSuccess { (activeConnection, loadedTasks) ->
                connection = activeConnection
                tasks = loadedTasks
                tasksInitialized = true
                enqueueOutput("Gradle sync finished: ${loadedTasks.size} tasks available.\n")
            }.onFailure { error ->
                syncError = error.message ?: "Unknown Gradle error"
                enqueueOutput("Gradle sync failed: ${syncError}\n")
            }

            isSyncing = false
        }
    }

    private fun enqueueOutput(text: String) {
        pendingOutput.trySend(text)
    }

    private companion object {
        const val MAX_OUTPUT_CHARS = 100_000
    }
}

private class ToolingLogOutputStream(
    private val onText: (String) -> Unit
) : OutputStream() {
    override fun write(value: Int) {
        onText(byteArrayOf(value.toByte()).toString(Charsets.UTF_8))
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (length > 0) {
            onText(String(bytes, offset, length, Charsets.UTF_8))
        }
    }
}
