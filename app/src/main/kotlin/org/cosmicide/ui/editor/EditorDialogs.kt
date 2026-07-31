package org.cosmicide.ui.editor

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.rosemoe.sora.text.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectTask
import org.cosmicide.project.ProjectTaskProvider

@Composable
internal fun TasksDialog(
    tasks: List<String>,
    isLoading: Boolean,
    loadError: String?,
    onDismiss: () -> Unit,
    onTaskSelected: (String) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Gradle Tasks") }, text = {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            loadError != null -> {
                Text(
                    text = "Failed to fetch tasks: $loadError",
                    color = MaterialTheme.colorScheme.error
                )
            }

            tasks.isEmpty() -> {
                Text("No Gradle tasks found")
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(tasks) { task ->
                        DropdownMenuItem(text = { Text(task) }, onClick = {
                            onTaskSelected(task)
                            onDismiss()
                        })
                    }
                }
            }
        }
    }, confirmButton = {}, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Close") }
    })
}

@Composable
internal fun ProjectTasksDialog(
    provider: ProjectTaskProvider,
    project: Project,
    onDismiss: () -> Unit,
    onTaskSelected: (ProjectTask) -> Unit
) {
    var tasks by remember(provider.id, project.root.absolutePath) {
        mutableStateOf(emptyList<ProjectTask>())
    }
    var isLoading by remember(provider.id, project.root.absolutePath) {
        mutableStateOf(true)
    }
    var loadError by remember(provider.id, project.root.absolutePath) {
        mutableStateOf<String?>(null)
    }
    var query by remember(provider.id, project.root.absolutePath) {
        mutableStateOf("")
    }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(provider.id, project.root.absolutePath, refreshKey) {
        isLoading = true
        loadError = null
        runCatching {
            withContext(Dispatchers.IO) { provider.tasks(project) }
        }.onSuccess {
            tasks = it.distinctBy(ProjectTask::id)
        }.onFailure {
            tasks = emptyList()
            loadError = it.message ?: "Unknown task discovery error"
        }
        isLoading = false
    }

    val filteredTasks = tasks.filter { task ->
        query.isBlank() ||
                task.label.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true) ||
                task.group.contains(query, ignoreCase = true) ||
                task.command.contains(query, ignoreCase = true)
    }
    val groupedTasks = filteredTasks.groupBy { it.group.ifBlank { "Other" } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(provider.displayName) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (provider.description.isNotBlank()) {
                    Text(
                        text = provider.description,
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    label = { Text("Filter tasks") },
                    singleLine = true
                )
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    loadError != null -> {
                        Text(
                            text = "Failed to load tasks: $loadError",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    filteredTasks.isEmpty() -> {
                        Text(if (query.isBlank()) "No tasks found" else "No matching tasks")
                    }

                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            groupedTasks.forEach { (group, groupTasks) ->
                                item {
                                    Text(
                                        text = group,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(groupTasks) { task ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(task.label)
                                                if (task.description.isNotBlank()) {
                                                    Text(
                                                        text = task.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme
                                                            .onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onTaskSelected(task)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { refreshKey += 1 }
            ) {
                Text("Refresh")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
internal fun GoToLineDialog(lineCount: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Go to Line") }, text = {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter { char -> char.isDigit() } },
            label = { Text("Line number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }, confirmButton = {
        Button(
            onClick = {
                val lineNumber = text.toIntOrNull()
                if (lineNumber != null && lineNumber in 1..lineCount) {
                    onConfirm(lineNumber)
                } else {
                    Toast.makeText(
                        context,
                        "Invalid line number. Must be between 1 and $lineCount.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, shapes = ButtonDefaults.shapes()
        ) {
            Text("Go")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
internal fun Statistics(content: Content, onDismiss: () -> Unit) {
    val bytes = content.toString().toByteArray().size
    val charCount = content.length

    AlertDialog(
        onDismissRequest = { onDismiss() },
        modifier = Modifier.fillMaxWidth(0.8f),
        title = { Text("Statistics") },
        text = {
            Column {
                Text("Byte Count: $bytes")
                Text("Character Count: $charCount")
                Text("Word Count: ${content.split(" ").size}")
                Text("Line Count: ${content.lineCount}")
            }
        },
        confirmButton = {
            TextButton(onDismiss) {
                Text("Dismiss")
            }
        },
        dismissButton = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}
