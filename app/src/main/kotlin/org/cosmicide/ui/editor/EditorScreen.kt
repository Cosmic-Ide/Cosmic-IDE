package org.cosmicide.ui.editor

import android.content.Context
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.editor.EditorPreviewPresentation
import org.cosmicide.editor.lsp.LspLogStore
import org.cosmicide.editor.lsp.disposeLspLanguage
import org.cosmicide.editor.preview.EditorPreviews
import org.cosmicide.model.EditorViewModel
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCommand
import org.cosmicide.project.ProjectTask
import java.io.File

private class EditorTabSession(
    val editor: CodeEditor
) {
    var initialized = false
    var isApplyingInitialContent = false
    var unsubscribeContentChanges: (() -> Unit)? = null

    fun release() {
        unsubscribeContentChanges?.invoke()
        editor.disposeLspLanguage()
        editor.release()
    }
}

private fun createEditorTabSession(
    context: Context,
    file: File,
    viewModel: EditorViewModel
): EditorTabSession {
    val editor = setCodeEditorFactory(context, CodeEditorState())
    val session = EditorTabSession(editor)
    val receipt = editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
        if (!session.isApplyingInitialContent) {
            viewModel.onDocumentContentChanged(file, editor.text.toString())
        }
    }
    session.unsubscribeContentChanges = receipt::unsubscribe
    return session
}

@Composable
fun EditorScreen(
    project: Project,
    viewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val projectSessionServices = LocalAppContainer.current.projectSessionServices
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var toolWindowHeightDp by rememberSaveable(project.root.absolutePath) {
        mutableFloatStateOf(CollapsedEditorToolWindowHeightDp)
    }
    var toolWindowSessionState by remember(project.root.absolutePath) {
        mutableStateOf(EditorToolWindowSessionState())
    }
    val projectCommands = remember(project.root.absolutePath) {
        projectSessionServices.commands(project)
    }
    val projectTaskProviders = remember(project.root.absolutePath) {
        projectSessionServices.taskProviders(project)
    }
    val projectSyncStrategy = remember(projectCommands) {
        resolveProjectSyncStrategy(projectCommands)
    }
    val projectSyncCommand =
        (projectSyncStrategy as? ProjectSyncStrategy.PluginCommand)?.command
    val isProjectSyncing =
        (projectSyncCommand != null && toolWindowSessionState.isProjectSyncInProgress)
    val openFiles = viewModel.openFiles
    val activeFile = viewModel.activeFile
    val lspLogs by LspLogStore.entries.collectAsStateWithLifecycle()
    val activePreviewProvider = activeFile?.let { EditorPreviews.providerFor(project, it) }
    val isPreviewOnly =
        activePreviewProvider?.presentation == EditorPreviewPresentation.PREVIEW_ONLY
    val editorSessions = remember(project.root.absolutePath) {
        mutableMapOf<File, EditorTabSession>()
    }
    val fallbackEditor = remember(project.root.absolutePath) {
        setCodeEditorFactory(context, CodeEditorState())
    }
    val activeEditorSession = activeFile
        ?.takeUnless { isPreviewOnly }
        ?.let { file ->
            editorSessions.getOrPut(file) {
                createEditorTabSession(context, file, viewModel)
            }
        }
    val editor = activeEditorSession?.editor ?: fallbackEditor
    val currentEditorContent = {
        if (isPreviewOnly) null else editor.text.toString()
    }

    val openTerminalSession: (String, String, List<String>?) -> Unit =
        { title, command, arguments ->
            viewModel.saveActiveDocument(currentEditorContent())
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
            openTerminalSession(
                command.label,
                "bash",
                projectCommandShellArguments(command.command)
            )
        }
    }
    val runProjectTask: (ProjectTask) -> Unit = { task ->
        openTerminalSession(task.label, "bash", projectCommandShellArguments(task.command))
    }

    val openFile = { newFile: File ->
        viewModel.openFile(newFile, currentEditorContent())
    }

    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(activeFile, activeEditorSession, isPreviewOnly) {
        activeFile?.let { file ->
            val session = activeEditorSession ?: return@let
            if (session.initialized) return@let
            val content = viewModel.cachedContent(file)
                ?: withContext(Dispatchers.IO) { file.readText() }
            viewModel.ensureDocument(file, content)

            session.isApplyingInitialContent = true
            try {
                session.editor.setText(content)
            } finally {
                session.isApplyingInitialContent = false
            }
            session.editor.applyEditorSettings(project, file, colorScheme)
            session.initialized = true
        }
    }

    DisposableEffect(editorSessions, fallbackEditor) {
        onDispose {
            editorSessions.values.forEach(EditorTabSession::release)
            editorSessions.clear()
            fallbackEditor.release()
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
                    scope.launch { drawerState.close() }
                })
            }
        }, gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            topBar = {
                Column {
                    EditorToolbar(
                        project = project,
                        file = activeFile,
                        editor = editor,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        projectCommands = projectCommands,
                        onRunProjectCommand = runProjectCommand,
                        taskProviders = projectTaskProviders,
                        onRunProjectTask = runProjectTask,
                        onOpenTerminal = {
                            openTerminalSession("Terminal", "bash", listOf("-i"))
                        }
                    )

                    if (isProjectSyncing) {
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
                                                viewModel.closeTab(file, currentEditorContent())
                                                editorSessions.remove(file)?.release()
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
                    lspLogs = lspLogs.joinToString("\n", transform = { it.displayText() }),
                    projectSyncCommand = projectSyncCommand,
                    state = toolWindowSessionState,
                    heightDp = toolWindowHeightDp,
                    onStateChange = { toolWindowSessionState = it },
                    onHeightChange = { toolWindowHeightDp = it },
                    editorContent = {
                        if (activeFile != null) {
                            if (isPreviewOnly) {
                                PreviewProviderContent(
                                    provider = activePreviewProvider,
                                    project = project,
                                    file = activeFile,
                                    content = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                TextEditorContent(
                                    editor = editor,
                                    project = project,
                                    file = activeFile,
                                    previewProvider = activePreviewProvider
                                )
                            }
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
