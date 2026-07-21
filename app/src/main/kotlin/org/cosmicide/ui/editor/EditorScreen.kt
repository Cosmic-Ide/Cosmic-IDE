package org.cosmicide.ui.editor

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.cascade.CascadeDropdownMenu
import org.cosmicide.common.Prefs
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.model.EditorToolingViewModel
import org.cosmicide.model.EditorViewModel
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCommand
import org.cosmicide.project.ProjectCommandKind
import org.cosmicide.project.ProjectExtensionPoints
import org.cosmicide.tooling.ToolingServerManager
import org.cosmicide.util.ProjectHandler
import org.cosmicide.util.jdksDir
import java.io.File
import kotlin.time.ExperimentalTime

sealed class TreeDialogState {
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

@Composable
fun EditorScreen(
    project: Project,
    viewModel: EditorViewModel = viewModel()
) {
    val state = remember { CodeEditorState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val toolingViewModel: EditorToolingViewModel = viewModel(
        key = "editor-tooling:${project.root.absolutePath}"
    )
    var toolWindowHeightDp by rememberSaveable(project.root.absolutePath) {
        mutableStateOf(CollapsedEditorToolWindowHeightDp)
    }
    var selectedToolWindowTabId by remember(project.root.absolutePath) {
        mutableStateOf(SyncToolWindowTabId)
    }
    var buildSessions by remember(project.root.absolutePath) {
        mutableStateOf<List<EditorBuildSession>>(emptyList())
    }
    var nextBuildSessionId by remember(project.root.absolutePath) {
        mutableIntStateOf(0)
    }
    val projectCommands = remember(project.root.absolutePath) {
        CosmicPluginHost.enabledExtensions(ProjectExtensionPoints.COMMAND_PROVIDER)
            .flatMap { provider -> provider.commands(project) }
    }
    val hasGradleWrapper = remember(project.root.absolutePath) {
        project.root.resolve("gradlew").isFile
    }
    val projectSyncCommand = projectCommands.firstOrNull {
        it.kind == ProjectCommandKind.SYNC
    }.takeIf { !hasGradleWrapper }
    var projectSyncRunId by remember(project.root.absolutePath) { mutableIntStateOf(0) }
    var projectSyncStatus by remember(project.root.absolutePath) { mutableStateOf("Running") }

    val openFiles = viewModel.openFiles
    val activeFile = viewModel.activeFile

    val editor = remember {
        setCodeEditorFactory(context = context, state = state)
    }
    val isApplyingEditorContent = remember { mutableStateOf(false) }

    val runGradleTask: (String) -> Unit = { task ->
        viewModel.saveActiveDocument(editor.text.toString())
        val existingSession = buildSessions.firstOrNull {
            it.command == null && it.task == task
        }
        if (existingSession != null) {
            buildSessions = buildSessions.map { session ->
                if (session.id == existingSession.id) {
                    session.copy(runId = session.runId + 1, status = "Running")
                } else {
                    session
                }
            }
            selectedToolWindowTabId = existingSession.tabId
        } else {
            val session = EditorBuildSession(id = ++nextBuildSessionId, task = task)
            buildSessions = buildSessions + session
            selectedToolWindowTabId = session.tabId
        }
        if (toolWindowHeightDp <= CollapsedEditorToolWindowHeightDp) {
            toolWindowHeightDp = DefaultEditorToolWindowHeightDp
        }
    }

    val openTerminalSession: (String, String, List<String>?) -> Unit =
        { title, command, arguments ->
            viewModel.saveActiveDocument(editor.text.toString())
            val session = EditorBuildSession(
                id = ++nextBuildSessionId,
                task = title,
                command = command,
                arguments = arguments
            )
            buildSessions = buildSessions + session
            selectedToolWindowTabId = session.tabId
            if (toolWindowHeightDp <= CollapsedEditorToolWindowHeightDp) {
                toolWindowHeightDp = DefaultEditorToolWindowHeightDp
            }
        }

    val rerunProjectSync: () -> Unit = {
        projectSyncRunId++
        projectSyncStatus = "Running"
        selectedToolWindowTabId = SyncToolWindowTabId
        if (toolWindowHeightDp <= CollapsedEditorToolWindowHeightDp) {
            toolWindowHeightDp = DefaultEditorToolWindowHeightDp
        }
    }

    val runProjectCommand: (ProjectCommand) -> Unit = { command ->
        if (projectSyncCommand?.id == command.id) {
            rerunProjectSync()
        } else {
            openTerminalSession(command.label, "bash", listOf("-lc", command.command))
        }
    }

    LaunchedEffect(project.root.absolutePath, hasGradleWrapper) {
        ProjectHandler.setProject(project)
        if (hasGradleWrapper) {
            toolingViewModel.initialize(context, project.root)
        } else {
            ToolingServerManager.stopCurrent()
        }
    }

    val openFile = { newFile: File ->
        viewModel.openFile(newFile, editor.text.toString())
    }

    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(activeFile) {
        activeFile?.let { file ->
            val content = viewModel.cachedContent(file)
                ?: withContext(Dispatchers.IO) { file.readText() }
            viewModel.ensureDocument(file, content)

            isApplyingEditorContent.value = true
            try {
                editor.setText(content)
            } finally {
                isApplyingEditorContent.value = false
            }
            editor.applyEditorSettings(file, colorScheme)
        }
    }

    DisposableEffect(editor) {
        val receipt = editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            if (!isApplyingEditorContent.value) {
                viewModel.onActiveContentChanged(editor.text.toString())
            }
        }

        onDispose {
            receipt.unsubscribe()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState, drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Project Explorer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()
                ProjectTreeView(rootDir = project.root, onFileClick = { file ->
                    openFile(file)
                    scope.launch { drawerState.close() }
                }, onExecuteFile = { file ->
                    val executionPath =
                        file.absolutePath.replace(project.srcDir.absolutePath + "/", "")
                    ProjectHandler.clazz = executionPath.substringBeforeLast('.')
                    runGradleTask("build")
                    scope.launch { drawerState.close() }
                })
            }
        }) {
        Scaffold(
            topBar = {
                Column {
                    EditorToolbar(
                        project = project,
                        file = activeFile,
                        editor = editor,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        tasks = toolingViewModel.tasks,
                        isGradleSyncing = hasGradleWrapper && toolingViewModel.isSyncing,
                        gradleSyncError = toolingViewModel.syncError,
                        onResyncGradle = { toolingViewModel.resyncGradle(context) },
                        hasGradleWrapper = hasGradleWrapper,
                        onRunGradleTask = runGradleTask,
                        projectCommands = projectCommands,
                        onRunProjectCommand = runProjectCommand,
                        onOpenTerminal = {
                            openTerminalSession("Terminal", "bash", listOf("-i"))
                        }
                    )

                    if (hasGradleWrapper && toolingViewModel.isSyncing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    if (openFiles.isNotEmpty()) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = openFiles.indexOf(activeFile).coerceAtLeast(0),
                            edgePadding = 8.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            divider = { HorizontalDivider() }) {
                            openFiles.forEach { file ->
                                val isSelected = file == activeFile
                                Tab(
                                    selected = isSelected, onClick = { openFile(file) }) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp, vertical = 10.dp
                                        )
                                    ) {
                                        Text(
                                            file.name, style = MaterialTheme.typography.labelMedium
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                viewModel.closeTab(file, editor.text.toString())
                                            }, modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close Tab",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(thickness = 1.dp)
                    }
                }
            }) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                EditorToolWindowLayout(
                    project = project,
                    toolingOutput = toolingViewModel.output,
                    useGradleSync = hasGradleWrapper,
                    projectSyncCommand = projectSyncCommand,
                    projectSyncRunId = projectSyncRunId,
                    projectSyncStatus = projectSyncStatus,
                    selectedTabId = selectedToolWindowTabId,
                    heightDp = toolWindowHeightDp,
                    buildSessions = buildSessions,
                    onSelectTab = { tabId ->
                        if (selectedToolWindowTabId == tabId &&
                            toolWindowHeightDp > CollapsedEditorToolWindowHeightDp
                        ) {
                            toolWindowHeightDp = CollapsedEditorToolWindowHeightDp
                        } else {
                            selectedToolWindowTabId = tabId
                            if (toolWindowHeightDp <= CollapsedEditorToolWindowHeightDp) {
                                toolWindowHeightDp = DefaultEditorToolWindowHeightDp
                            }
                        }
                    },
                    onHeightChange = { toolWindowHeightDp = it },
                    onCloseBuild = { sessionId ->
                        val remaining = buildSessions.filterNot { it.id == sessionId }
                        val closedTabId = buildSessions
                            .firstOrNull { it.id == sessionId }
                            ?.tabId
                        buildSessions = remaining
                        if (selectedToolWindowTabId == closedTabId) {
                            selectedToolWindowTabId = remaining.lastOrNull()?.tabId
                                ?: SyncToolWindowTabId
                        }
                    },
                    onRerunBuild = { sessionId ->
                        buildSessions = buildSessions.map { session ->
                            if (session.id == sessionId) {
                                session.copy(runId = session.runId + 1, status = "Running")
                            } else {
                                session
                            }
                        }
                    },
                    onBuildStatusChange = { sessionId, status ->
                        buildSessions = buildSessions.map { session ->
                            if (session.id == sessionId) session.copy(status = status)
                            else session
                        }
                    },
                    onRerunProjectSync = rerunProjectSync,
                    onProjectSyncStatusChange = { projectSyncStatus = it },
                    editorContent = {
                        if (activeFile != null) {
                            TextEditorContent(editor)
                        } else {
                            EmptyWorkspaceState(
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectTreeView(
    rootDir: File, onFileClick: (File) -> Unit, onExecuteFile: (File) -> Unit
) {
    val context = LocalContext.current
    var expandedDirs by remember { mutableStateOf(setOf(rootDir)) }

    var contextMenuFile by remember { mutableStateOf<File?>(null) }
    var activeDialog by remember { mutableStateOf<TreeDialogState?>(null) }
    var treeTrigger by remember { mutableIntStateOf(0) }

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
                    val cleanName = name.replace("\\.", "") + state.type.suffix
                    val newTarget = state.parent.resolve(cleanName)
                    if (state.type == TreeDialogState.CreateType.FOLDER) {
                        newTarget.mkdirs()
                    } else {
                        newTarget.createNewFile()
                    }
                    if (state.parent.isDirectory && !expandedDirs.contains(state.parent)) {
                        toggleExpand(state.parent)
                    }
                    treeTrigger++
                    activeDialog = null
                })
        }

        is TreeDialogState.Rename -> {
            TreeInputDialog(
                title = "Rename",
                initialValue = state.target.name,
                onDismiss = { activeDialog = null },
                onConfirm = { name ->
                    val cleanName = name.replace("\\.", "")
                    state.target.renameTo(state.target.parentFile!!.resolve(cleanName))
                    treeTrigger++
                    activeDialog = null
                })
        }

        is TreeDialogState.Delete -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Delete") },
                text = { Text("Are you sure you want to delete ${state.target.name}?") },
                confirmButton = {
                    TextButton(onClick = {
                        state.target.deleteRecursively()
                        treeTrigger++
                        activeDialog = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Cancel") }
                })
        }

        null -> {}
    }
}

@Composable
fun TreeInputDialog(
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

@Composable
fun EmptyWorkspaceState(onOpenDrawer: () -> Unit) {
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
fun EditorToolbar(
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

@Composable
fun TasksDialog(
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
fun GoToLineDialog(lineCount: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
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
fun ProgramArgumentDialog(
    title: String, savedArgs: List<String>, onSave: (List<String>) -> Unit, onDismiss: () -> Unit
) {
    var args by remember { mutableStateOf(savedArgs.joinToString(" ")) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        OutlinedTextField(
            value = args,
            label = { Text(title) },
            singleLine = true,
            onValueChange = { args = it })
    }, confirmButton = {
        Button(
            onClick = {
                onSave(args.split(' '))
                onDismiss()
            }, shapes = ButtonDefaults.shapes()
        ) {
            Text("Save")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Statistics(content: Content, onDismiss: () -> Unit) {
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

@Composable
private fun TextEditorContent(editor: CodeEditor) {
    var charset by remember { mutableStateOf("UTF-8") }
    var position by remember { mutableStateOf("1:1") }
    var statsDialogShown by remember { mutableStateOf(false) }

    DisposableEffect(editor) {
        val receipt = editor.subscribeAlways<SelectionChangeEvent> { event ->
            position = event.left.let {
                "${it.line + 1}:${it.column}"
            }
        }

        onDispose {
            receipt.unsubscribe()
        }
    }

    Column {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = position,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable(true) {
                        statsDialogShown = true
                    },
                text = charset,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End
            )
        }
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { editor }, onRelease = {
            editor.release()
        })
    }

    if (statsDialogShown) {
        Statistics(editor.text) {
            statsDialogShown = false
        }
    }
}
