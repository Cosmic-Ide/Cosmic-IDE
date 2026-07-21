package org.cosmicide.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.rosemoe.sora.event.ContentChangeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.model.EditorToolingViewModel
import org.cosmicide.model.EditorViewModel
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCommand
import java.io.File

@Composable
fun EditorScreen(
    project: Project,
    viewModel: EditorViewModel = viewModel()
) {
    val state = remember { CodeEditorState() }
    val context = LocalContext.current
    val projectSessionServices = LocalAppContainer.current.projectSessionServices
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val toolingViewModel: EditorToolingViewModel = viewModel(
        key = "editor-tooling:${project.root.absolutePath}"
    )
    var toolWindowHeightDp by rememberSaveable(project.root.absolutePath) {
        mutableStateOf(CollapsedEditorToolWindowHeightDp)
    }
    var toolWindowSessionState by remember(project.root.absolutePath) {
        mutableStateOf(EditorToolWindowSessionState())
    }
    val projectCommands = remember(project.root.absolutePath) {
        projectSessionServices.commands(project)
    }
    val hasGradleWrapper = remember(project.root.absolutePath) {
        project.root.resolve("gradlew").isFile
    }
    val projectSyncStrategy = remember(hasGradleWrapper, projectCommands) {
        resolveProjectSyncStrategy(hasGradleWrapper, projectCommands)
    }
    val useGradleSync = projectSyncStrategy is ProjectSyncStrategy.GradleWrapper
    val projectSyncCommand =
        (projectSyncStrategy as? ProjectSyncStrategy.PluginCommand)?.command
    val openFiles = viewModel.openFiles
    val activeFile = viewModel.activeFile

    val editor = remember {
        setCodeEditorFactory(context = context, state = state)
    }
    val isApplyingEditorContent = remember { mutableStateOf(false) }

    val runGradleTask: (String) -> Unit = { task ->
        viewModel.saveActiveDocument(editor.text.toString())
        toolWindowSessionState = toolWindowSessionState.openGradleTask(task)
        if (toolWindowHeightDp <= CollapsedEditorToolWindowHeightDp) {
            toolWindowHeightDp = DefaultEditorToolWindowHeightDp
        }
    }

    val openTerminalSession: (String, String, List<String>?) -> Unit =
        { title, command, arguments ->
            viewModel.saveActiveDocument(editor.text.toString())
            toolWindowSessionState = toolWindowSessionState.openTerminal(
                title = title,
                command = command,
                arguments = arguments
            )
            if (toolWindowHeightDp <= CollapsedEditorToolWindowHeightDp) {
                toolWindowHeightDp = DefaultEditorToolWindowHeightDp
            }
        }

    val rerunProjectSync: () -> Unit = {
        toolWindowSessionState = toolWindowSessionState.rerunProjectSync()
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

    LaunchedEffect(project.root.absolutePath, useGradleSync) {
        if (useGradleSync) {
            toolingViewModel.initialize(context, project.root)
        } else {
            projectSessionServices.stopTooling()
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
            editor.applyEditorSettings(project, file, colorScheme)
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
                }, onExecuteFile = {
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
                        isGradleSyncing = useGradleSync && toolingViewModel.isSyncing,
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

                    if (useGradleSync && toolingViewModel.isSyncing) {
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
                    useGradleSync = useGradleSync,
                    projectSyncCommand = projectSyncCommand,
                    projectSyncRunId = toolWindowSessionState.projectSyncRunId,
                    projectSyncStatus = toolWindowSessionState.projectSyncStatus,
                    selectedTabId = toolWindowSessionState.selectedTabId,
                    heightDp = toolWindowHeightDp,
                    buildSessions = toolWindowSessionState.buildSessions,
                    onSelectTab = { tabId ->
                        if (toolWindowSessionState.selectedTabId == tabId &&
                            toolWindowHeightDp > CollapsedEditorToolWindowHeightDp
                        ) {
                            toolWindowHeightDp = CollapsedEditorToolWindowHeightDp
                        } else {
                            toolWindowSessionState = toolWindowSessionState.selectTab(tabId)
                            if (toolWindowHeightDp <= CollapsedEditorToolWindowHeightDp) {
                                toolWindowHeightDp = DefaultEditorToolWindowHeightDp
                            }
                        }
                    },
                    onHeightChange = { toolWindowHeightDp = it },
                    onCloseBuild = { sessionId ->
                        toolWindowSessionState = toolWindowSessionState.closeBuild(sessionId)
                    },
                    onRerunBuild = { sessionId ->
                        toolWindowSessionState = toolWindowSessionState.rerunBuild(sessionId)
                    },
                    onBuildStatusChange = { sessionId, status ->
                        toolWindowSessionState =
                            toolWindowSessionState.updateBuildStatus(sessionId, status)
                    },
                    onRerunProjectSync = rerunProjectSync,
                    onProjectSyncStatusChange = { status ->
                        toolWindowSessionState =
                            toolWindowSessionState.updateProjectSyncStatus(status)
                    },
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
