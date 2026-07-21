package org.cosmicide.ui.editor

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.cascade.CascadeDropdownMenu
import java.io.File

internal sealed class TreeDialogState {
    data class Create(val parent: File, val type: CreateType) : TreeDialogState()
    data class Rename(val target: File) : TreeDialogState()
    data class Delete(val target: File) : TreeDialogState()

    enum class CreateType(val title: String, val suffix: String = "") {
        KOTLIN_CLASS("Create Kotlin Class", ".kt"), JAVA_CLASS(
            "Create Java Class",
            ".java"
        ),
        FOLDER("Create Folder"), FILE("Create File")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProjectTreeView(
    rootDir: File, onFileClick: (File) -> Unit, onExecuteFile: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileOperations = remember(rootDir.absolutePath) {
        ProjectTreeFileOperations(rootDir)
    }
    var expandedDirs by remember { mutableStateOf(setOf(rootDir)) }

    var contextMenuFile by remember { mutableStateOf<File?>(null) }
    var activeDialog by remember { mutableStateOf<TreeDialogState?>(null) }
    var operationError by remember { mutableStateOf<String?>(null) }
    var treeTrigger by remember { mutableIntStateOf(0) }

    fun runMutation(operation: () -> Unit, onSuccess: () -> Unit = {}) {
        scope.launch {
            val failure = withContext(Dispatchers.IO) {
                runCatching(operation).exceptionOrNull()
            }
            if (failure == null) {
                treeTrigger++
                activeDialog = null
                onSuccess()
            } else {
                operationError = failure.message ?: "Project operation failed"
            }
        }
    }

    val toggleExpand = { dir: File ->
        if (expandedDirs.contains(dir)) {
            expandedDirs = expandedDirs - dir
        } else {
            val toExpand = mutableSetOf(dir)
            var current = dir

            while (true) {
                val kids = current.listFiles()
                if (kids != null && kids.size == 1 && kids[0].isDirectory) {
                    current = kids[0]
                    toExpand.add(current)
                } else {
                    break
                }
            }
            expandedDirs = expandedDirs + toExpand
        }
    }

    val treeItems = remember(expandedDirs, rootDir, treeTrigger) {
        val list = mutableListOf<Pair<File, Int>>()
        fun traverse(dir: File, depth: Int) {
            val children =
                dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: return
            for (child in children) {
                list.add(child to depth)
                if (child.isDirectory && expandedDirs.contains(child)) {
                    traverse(child, depth + 1)
                }
            }
        }
        traverse(rootDir, 0)
        list
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(treeItems, key = { it.first.absolutePath }) { (file, depth) ->
            val isExpanded = expandedDirs.contains(file)

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {
                            if (file.isDirectory) toggleExpand(file) else onFileClick(file)
                        }, onLongClick = {
                            contextMenuFile = file
                        })
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                        .padding(start = (depth * 16).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (file.isDirectory) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(18.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (contextMenuFile == file) {
                    CascadeDropdownMenu(
                        expanded = true, onDismissRequest = { contextMenuFile = null }) {
                        if (file.isDirectory) {
                            // Nesting creation options inside a fluid sub-menu
                            DropdownMenuItem(text = { Text("New...") }, children = {
                                DropdownMenuItem(text = { Text("Kotlin Class") }, onClick = {
                                    activeDialog = TreeDialogState.Create(
                                        file, TreeDialogState.CreateType.KOTLIN_CLASS
                                    )
                                    contextMenuFile = null
                                })
                                DropdownMenuItem(text = { Text("Java Class") }, onClick = {
                                    activeDialog = TreeDialogState.Create(
                                        file, TreeDialogState.CreateType.JAVA_CLASS
                                    )
                                    contextMenuFile = null
                                })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Folder") }, onClick = {
                                    activeDialog = TreeDialogState.Create(
                                        file, TreeDialogState.CreateType.FOLDER
                                    )
                                    contextMenuFile = null
                                })
                                DropdownMenuItem(text = { Text("File") }, onClick = {
                                    activeDialog = TreeDialogState.Create(
                                        file, TreeDialogState.CreateType.FILE
                                    )
                                    contextMenuFile = null
                                })
                            })
                        } else {
                            if (file.extension == "kt" || file.extension == "java") {
                                DropdownMenuItem(text = { Text("Execute") }, onClick = {
                                    onExecuteFile(file)
                                    contextMenuFile = null
                                })
                            }
                            DropdownMenuItem(text = { Text("Open External") }, onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.provider", file
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open with"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                contextMenuFile = null
                            })
                        }

                        HorizontalDivider()

                        DropdownMenuItem(text = { Text("Rename") }, onClick = {
                            activeDialog = TreeDialogState.Rename(file)
                            contextMenuFile = null
                        })
                        DropdownMenuItem(text = {
                            Text(
                                "Delete", color = MaterialTheme.colorScheme.error
                            )
                        }, onClick = {
                            activeDialog = TreeDialogState.Delete(file)
                            contextMenuFile = null
                        })
                    }
                }
            }
        }
    }

    when (val state = activeDialog) {
        is TreeDialogState.Create -> {
            TreeInputDialog(
                title = state.type.title,
                initialValue = "",
                suffix = state.type.suffix,
                onDismiss = { activeDialog = null },
                onConfirm = { name ->
                    runMutation(
                        operation = {
                            fileOperations.create(
                                parentDirectory = state.parent,
                                name = name,
                                suffix = state.type.suffix,
                                directory = state.type == TreeDialogState.CreateType.FOLDER
                            )
                        },
                        onSuccess = {
                            if (state.parent.isDirectory &&
                                !expandedDirs.contains(state.parent)
                            ) {
                                toggleExpand(state.parent)
                            }
                        }
                    )
                })
        }

        is TreeDialogState.Rename -> {
            TreeInputDialog(
                title = "Rename",
                initialValue = state.target.name,
                onDismiss = { activeDialog = null },
                onConfirm = { name ->
                    runMutation(operation = {
                        fileOperations.rename(state.target, name)
                    })
                })
        }

        is TreeDialogState.Delete -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Delete") },
                text = { Text("Are you sure you want to delete ${state.target.name}?") },
                confirmButton = {
                    TextButton(onClick = {
                        runMutation(operation = {
                            fileOperations.delete(state.target)
                        })
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Cancel") }
                })
        }

        null -> {}
    }

    operationError?.let { message ->
        AlertDialog(
            onDismissRequest = { operationError = null },
            title = { Text("Project operation failed") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { operationError = null }) { Text("OK") }
            }
        )
    }
}

@Composable
internal fun TreeInputDialog(
    title: String,
    initialValue: String,
    suffix: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Name") },
            suffix = if (suffix.isNotEmpty()) {
                { Text(suffix) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth())
    }, confirmButton = {
        Button(
            onClick = { if (text.isNotBlank()) onConfirm(text) },
            enabled = text.isNotBlank(),
            shapes = ButtonDefaults.shapes()
        ) { Text("Confirm") }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancel") }
    })
}
