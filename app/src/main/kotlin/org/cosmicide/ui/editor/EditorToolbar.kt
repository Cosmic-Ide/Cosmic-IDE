package org.cosmicide.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways
import me.saket.cascade.CascadeDropdownMenu
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCommand
import org.cosmicide.project.ProjectCommandKind
import java.io.File
import kotlin.time.ExperimentalTime

@Composable
internal fun EmptyWorkspaceState(onOpenDrawer: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Text(
            text = "No file open",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )
        Button(onClick = onOpenDrawer, shapes = ButtonDefaults.shapes()) {
            Text("Open Project Explorer")
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTime::class
)
@Composable
internal fun EditorToolbar(
    project: Project,
    file: File?,
    editor: CodeEditor,
    tasks: List<String>,
    isGradleSyncing: Boolean,
    gradleSyncError: String?,
    onResyncGradle: () -> Unit,
    hasGradleWrapper: Boolean,
    onOpenDrawer: () -> Unit,
    onRunGradleTask: (String) -> Unit,
    projectCommands: List<ProjectCommand>,
    onRunProjectCommand: (ProjectCommand) -> Unit,
    onOpenTerminal: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var showProgramArgsDialog by remember { mutableStateOf(false) }
    var showJREArgsDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showTasksDialog by remember { mutableStateOf(false) }

    fun editorCanUndo(): Boolean {
        return editor.text.undoManager.canUndo()
    }

    fun editorCanRedo(): Boolean {
        return editor.text.undoManager.canRedo()
    }

    var canUndo by remember { mutableStateOf(editorCanUndo()) }
    var canRedo by remember { mutableStateOf(editorCanRedo()) }

    fun refreshHistoryState() {
        canUndo = editorCanUndo()
        canRedo = editorCanRedo()
    }

    LaunchedEffect(file) {
        refreshHistoryState()
    }

    DisposableEffect(editor) {
        val receipt = editor.subscribeAlways<ContentChangeEvent> {
            refreshHistoryState()
        }

        onDispose {
            receipt.unsubscribe()
        }
    }

    val contributedRunCommand = projectCommands.firstOrNull {
        it.kind == ProjectCommandKind.RUN
    }
    val contributedSyncCommand = projectCommands.firstOrNull {
        it.kind == ProjectCommandKind.SYNC
    }

    TopAppBar(title = {
        Text(
            text = file?.name ?: "Cosmic IDE",
            style = MaterialTheme.typography.titleMediumEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }, navigationIcon = {
        IconButton(onClick = onOpenDrawer) {
            Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
        }
    }, actions = {
        IconButton(
            enabled = contributedRunCommand != null || hasGradleWrapper,
            onClick = {
                if (contributedRunCommand != null) onRunProjectCommand(contributedRunCommand)
                else onRunGradleTask("run")
            }
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = contributedRunCommand?.label
                    ?: if (hasGradleWrapper) "Run Gradle task" else "No run command configured",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = {
            editor.undo()
            refreshHistoryState()
        }, enabled = file != null) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = if (canUndo) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
        }
        IconButton(onClick = {
            editor.redo()
            refreshHistoryState()
        }, enabled = file != null) {
            Icon(
                Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                tint = if (canRedo) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
        }

        Box {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Editor Options")
            }

            CascadeDropdownMenu(
                expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Execution") }, children = {
                    DropdownMenuItem(text = { Text("Program Arguments") }, onClick = {
                        showProgramArgsDialog = true
                        showMenu = false
                    })
                    DropdownMenuItem(text = { Text("Runtime Arguments") }, onClick = {
                        showJREArgsDialog = true
                        showMenu = false
                    })
                    DropdownMenuItem(text = { Text("Terminal") }, onClick = {
                        onOpenTerminal()
                        showMenu = false
                    })
                    if (contributedSyncCommand != null) {
                        DropdownMenuItem(text = { Text(contributedSyncCommand.label) }, onClick = {
                            onRunProjectCommand(contributedSyncCommand)
                            showMenu = false
                        })
                    }
                })
                if (projectCommands.isNotEmpty()) {
                    DropdownMenuItem(text = { Text("Project Commands") }, children = {
                        projectCommands.forEach { command ->
                            DropdownMenuItem(
                                text = { Text(command.label) },
                                onClick = {
                                    onRunProjectCommand(command)
                                    showMenu = false
                                }
                            )
                        }
                    })
                }
                DropdownMenuItem(text = { Text("Editor") }, children = {
                    DropdownMenuItem(text = { Text("Format") }, onClick = {
                        editor.formatCodeAsync()
                        showMenu = false
                    })
                    DropdownMenuItem(text = { Text("Go To Line") }, onClick = {
                        showGoToLineDialog = true
                        showMenu = false
                    })
                    DropdownMenuItem(text = { Text("View Statistics") }, onClick = {
                        showStatsDialog = true
                        showMenu = false
                    })
                })
                DropdownMenuItem(text = { Text("File Options") }, children = {
                    DropdownMenuItem(text = { Text("View Statistics") }, onClick = {
                        showMenu = false
                    })
                })
                if (hasGradleWrapper) DropdownMenuItem(text = { Text("Gradle") }, children = {
                    DropdownMenuItem(text = { Text("Tasks") }, onClick = {
                        showTasksDialog = true
                        showMenu = false
                    })
                    DropdownMenuItem(
                        text = { Text("Resync Gradle") },
                        enabled = !isGradleSyncing,
                        onClick = {
                            onResyncGradle()
                            showMenu = false
                        }
                    )
                })
            }
        }
    })

    if (showTasksDialog) {
        TasksDialog(
            tasks = tasks,
            isLoading = isGradleSyncing,
            loadError = gradleSyncError,
            onDismiss = { showTasksDialog = false },
            onTaskSelected = { task ->
                onRunGradleTask(task)
            }
        )
    }

    if (showGoToLineDialog) {
        GoToLineDialog(
            lineCount = editor.lineCount,
            onDismiss = { showGoToLineDialog = false },
            onConfirm = { lineNumber ->
                editor.jumpToLine(lineNumber - 1)
                showGoToLineDialog = false
            })
    }

    if (showProgramArgsDialog) {
        ProgramArgumentDialog(
            title = "Program Arguments",
            savedArgs = project.args,
            onSave = { args ->
                project.args = args
            },
            onDismiss = {
                showProgramArgsDialog = false
            })
    }

    if (showJREArgsDialog) {
        ProgramArgumentDialog(
            title = "Runtime Arguments",
            savedArgs = project.runtimeArgs,
            onSave = { args ->
                project.runtimeArgs = args
            },
            onDismiss = {
                showJREArgsDialog = false
            })
    }

    if (showStatsDialog) {
        Statistics(
            content = editor.text,
            onDismiss = { showStatsDialog = false }
        )
    }
}
