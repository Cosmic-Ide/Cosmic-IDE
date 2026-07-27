package org.cosmicide.ui.editor

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.cosmicide.R
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCommand
import org.cosmicide.ui.compile.CommandTerminal
import org.cosmicide.ui.compile.GradleTaskTerminal
import org.cosmicide.ui.compile.TerminalSessionHandle

internal const val SyncToolWindowTabId = "sync"
internal const val LspLogsToolWindowTabId = "lsp-logs"
internal const val CollapsedEditorToolWindowHeightDp = 64f
internal const val DefaultEditorToolWindowHeightDp = 280f

internal data class EditorBuildSession(
    val id: Int,
    val task: String,
    val command: String? = null,
    val arguments: List<String>? = null,
    val runId: Int = 0,
    val status: String = "Running"
) {
    val tabId: String
        get() = "build:$id"
}

@Composable
internal fun EditorToolWindowLayout(
    project: Project,
    syncOutput: Content?,
    isToolingSyncRunning: Boolean,
    onRerunToolingSync: () -> Unit,
    onStopToolingSync: () -> Unit,
    lspLogs: String,
    projectSyncCommand: ProjectCommand?,
    state: EditorToolWindowSessionState,
    heightDp: Float,
    onStateChange: (EditorToolWindowSessionState) -> Unit,
    onHeightChange: (Float) -> Unit,
    editorContent: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxToolWindowHeight = maxHeight.value.coerceAtLeast(CollapsedEditorToolWindowHeightDp)
        val resolvedHeight = heightDp.coerceIn(
            CollapsedEditorToolWindowHeightDp,
            maxToolWindowHeight
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = resolvedHeight.dp)
            ) {
                editorContent()
            }

            EditorToolWindow(
                project = project,
                syncOutput = syncOutput,
                isToolingSyncRunning = isToolingSyncRunning,
                onRerunToolingSync = onRerunToolingSync,
                onStopToolingSync = onStopToolingSync,
                lspLogs = lspLogs,
                projectSyncCommand = projectSyncCommand,
                state = state,
                heightDp = resolvedHeight,
                onStateChange = onStateChange,
                onSelectTab = { tabId ->
                    if (state.selectedTabId == tabId &&
                        resolvedHeight > CollapsedEditorToolWindowHeightDp
                    ) {
                        onHeightChange(CollapsedEditorToolWindowHeightDp)
                    } else {
                        onStateChange(state.selectTab(tabId))
                        if (resolvedHeight <= CollapsedEditorToolWindowHeightDp) {
                            onHeightChange(DefaultEditorToolWindowHeightDp)
                        }
                    }
                },
                onResize = { dragAmountPx ->
                    val dragAmountDp = with(density) { dragAmountPx.toDp().value }
                    onHeightChange(
                        (resolvedHeight - dragAmountDp).coerceIn(
                            CollapsedEditorToolWindowHeightDp,
                            maxToolWindowHeight
                        )
                    )
                },
                onResizeFinished = {
                    if (resolvedHeight < CollapseSnapThresholdDp) {
                        onHeightChange(CollapsedEditorToolWindowHeightDp)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(resolvedHeight.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorToolWindow(
    project: Project,
    syncOutput: Content?,
    isToolingSyncRunning: Boolean,
    onRerunToolingSync: () -> Unit,
    onStopToolingSync: () -> Unit,
    lspLogs: String,
    projectSyncCommand: ProjectCommand?,
    state: EditorToolWindowSessionState,
    heightDp: Float,
    onStateChange: (EditorToolWindowSessionState) -> Unit,
    onSelectTab: (String) -> Unit,
    onResize: (Float) -> Unit,
    onResizeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTabId = state.selectedTabId
    val buildSessions = state.buildSessions
    val isExpanded = heightDp > CollapsedEditorToolWindowHeightDp + 1f
    val currentOnResize by rememberUpdatedState(onResize)
    val currentOnResizeFinished by rememberUpdatedState(onResizeFinished)
    val resizeGestureModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragEnd = { currentOnResizeFinished() },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                currentOnResize(dragAmount)
            }
        )
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 4.dp,
        shadowElevation = if (isExpanded) 8.dp else 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ResizeHandleHeightDp.dp)
                    .then(resizeGestureModifier)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {

                DragHandle(
                    modifier = Modifier
                        .widthIn(min = 48.dp, max = 72.dp)
                        .height(3.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            MaterialTheme.shapes.extraSmall
                        )
                        .padding(vertical = 2.dp)
                )
            }

            val selectedTabIndex = when {
                selectedTabId == SyncToolWindowTabId -> 0
                selectedTabId == LspLogsToolWindowTabId -> buildSessions.size + 1
                else -> buildSessions.indexOfFirst { it.tabId == selectedTabId }
                    .takeIf { it >= 0 }
                    ?.plus(1)
                    ?: 0
            }

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TabsHeightDp.dp)
                    .then(resizeGestureModifier),
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                divider = {}
            ) {
                Tab(
                    selected = selectedTabId == SyncToolWindowTabId,
                    onClick = { onSelectTab(SyncToolWindowTabId) },
                    text = { Text("Sync") }
                )

                buildSessions.forEach { session ->
                    Tab(
                        selected = selectedTabId == session.tabId,
                        onClick = { onSelectTab(session.tabId) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = session.task,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                                IconButton(
                                    onClick = {
                                        onStateChange(state.closeBuild(session.id))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close ${session.task} build",
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    )
                }

                Tab(
                    selected = selectedTabId == LspLogsToolWindowTabId,
                    onClick = { onSelectTab(LspLogsToolWindowTabId) },
                    text = { Text("LSP Logs") }
                )
            }

            if (isExpanded) {
                HorizontalDivider()
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                if (projectSyncCommand != null) {
                    ProjectSyncTab(
                        project = project,
                        command = projectSyncCommand,
                        runId = state.projectSyncRunId,
                        status = state.projectSyncStatus,
                        onRerun = {
                            onStateChange(state.rerunProjectSync())
                        },
                        onStop = {
                            onStateChange(state.stopProjectSync())
                        },
                        onStatusChange = { status ->
                            onStateChange(state.updateProjectSyncStatus(status))
                        },
                        modifier = Modifier.visibleToolWindowTab(
                            selectedTabId == SyncToolWindowTabId
                        )
                    )
                } else if (selectedTabId == SyncToolWindowTabId) {
                    if (syncOutput != null) {
                        ToolingSyncTab(
                            output = syncOutput,
                            isRunning = isToolingSyncRunning,
                            onRerun = onRerunToolingSync,
                            onStop = onStopToolingSync,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SyncTab(
                            output = remember {
                                Content("No project sync command is configured.").apply {
                                    setUndoEnabled(false)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                buildSessions.forEach { session ->
                    key(session.id) {
                        BuildTab(
                            project = project,
                            session = session,
                            onRerun = {
                                onStateChange(state.rerunBuild(session.id))
                            },
                            onStop = {
                                onStateChange(state.stopBuild(session.id))
                            },
                            onStatusChange = { status ->
                                onStateChange(state.updateBuildStatus(session.id, status))
                            },
                            modifier = Modifier.visibleToolWindowTab(
                                selectedTabId == session.tabId
                            )
                        )
                    }
                }
                if (selectedTabId == LspLogsToolWindowTabId) {
                    LspLogsTab(
                        output = lspLogs,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun Modifier.visibleToolWindowTab(selected: Boolean): Modifier {
    return fillMaxSize()
        .alpha(if (selected) 1f else 0f)
        .zIndex(if (selected) 1f else 0f)
}

@Composable
private fun ToolingSyncTab(
    output: Content,
    isRunning: Boolean,
    onRerun: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gradle sync", style = MaterialTheme.typography.labelLarge)
            Text(
                text = if (isRunning) "  ·  Running" else "  ·  Finished",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            SyncActionButton(
                isRunning = isRunning,
                isStopping = false,
                label = "Gradle sync",
                onRerun = onRerun,
                onStop = onStop
            )
        }
        HorizontalDivider()
        SyncTab(
            output = output,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun SyncTab(
    output: Content,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val context = LocalContext.current
    val editor = remember(output, backgroundColor, textColor) {
        CodeEditor(context).apply {
            setEditorLanguage(EmptyLanguage())
            setText(output)
            setEditable(false)
            setUndoEnabled(false)
            setLineNumberEnabled(false)
            setHighlightCurrentLine(false)
            setCursorAnimationEnabled(false)
            setWordwrap(false)
            setScrollBarEnabled(true)
            setInterceptParentHorizontalScrollIfNeeded(true)
            isFocusable = false
            isFocusableInTouchMode = false
            typefaceText = Typeface.MONOSPACE
            setTextSize(SYNC_OUTPUT_TEXT_SIZE_SP)
            colorScheme.apply {
                setColor(EditorColorScheme.WHOLE_BACKGROUND, backgroundColor)
                setColor(EditorColorScheme.TEXT_NORMAL, textColor)
            }
        }
    }

    AndroidView(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
        factory = { editor },
        onRelease = CodeEditor::release
    )
}

private const val SYNC_OUTPUT_TEXT_SIZE_SP = 14f

@Composable
private fun LspLogsTab(
    output: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(output, scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        SelectionContainer {
            Text(
                text = output.ifEmpty { "No LSP logs yet." },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ProjectSyncTab(
    project: Project,
    command: ProjectCommand,
    runId: Int,
    status: String,
    onRerun: () -> Unit,
    onStop: () -> Unit,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionHandle = remember(runId) { TerminalSessionHandle() }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(command.label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "  ·  $status",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            SyncActionButton(
                isRunning = status == "Running",
                isStopping = status == "Stopping",
                label = command.label,
                onRerun = onRerun,
                onStop = {
                    onStop()
                    sessionHandle.terminate()
                }
            )
        }
        HorizontalDivider()
        key(runId) {
            EmbeddedCommand(
                project = project,
                command = "bash",
                arguments = listOf("-lc", command.command),
                onExit = { exitCode ->
                    onStatusChange(if (exitCode == 0) "Finished" else "Exited ($exitCode)")
                },
                onError = onStatusChange,
                sessionHandle = sessionHandle,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun SyncActionButton(
    isRunning: Boolean,
    isStopping: Boolean,
    label: String,
    onRerun: () -> Unit,
    onStop: () -> Unit
) {
    val showStop = isRunning || isStopping
    IconButton(
        onClick = if (showStop) onStop else onRerun,
        enabled = !isStopping,
        modifier = Modifier.size(36.dp)
    ) {
        if (showStop) {
            Icon(
                Icons.Default.Stop,
                contentDescription = "Stop $label",
                tint = if (isStopping) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                } else {
                    Color.Red
                }
            )
        } else {
            Icon(Icons.Default.Refresh, contentDescription = "Rerun $label")
        }
    }
}

@Composable
private fun BuildTab(
    project: Project,
    session: EditorBuildSession,
    onRerun: () -> Unit,
    onStop: () -> Unit,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionHandle = remember(session.runId) { TerminalSessionHandle() }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.task,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "  ·  ${session.status}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    onStop()
                    sessionHandle.terminate()
                },
                enabled = session.status == "Running",
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop ${session.task}",
                    tint = Color.Red
                )
            }
            IconButton(onClick = onRerun, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Rerun ${session.task}")
            }
        }
        HorizontalDivider()

        key(session.runId) {
            if (session.command == null) {
                EmbeddedGradleBuild(
                    project = project,
                    task = session.task,
                    onSuccess = { onStatusChange("Finished") },
                    onError = onStatusChange,
                    sessionHandle = sessionHandle,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )
            } else {
                EmbeddedCommand(
                    project = project,
                    command = session.command,
                    arguments = session.arguments,
                    onExit = { exitCode ->
                        onStatusChange(if (exitCode == 0) "Finished" else "Exited ($exitCode)")
                    },
                    onError = onStatusChange,
                    sessionHandle = sessionHandle,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EmbeddedCommand(
    project: Project,
    command: String,
    arguments: List<String>?,
    onExit: (Int) -> Unit,
    onError: (String) -> Unit,
    sessionHandle: TerminalSessionHandle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val typeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.firacode_medium) ?: Typeface.MONOSPACE
    }
    var textSizeDp by rememberSaveable { mutableIntStateOf(12) }

    CommandTerminal(
        modifier = modifier,
        context = context,
        workingDirectory = project.root,
        commandLine = command,
        commandArguments = arguments,
        colorScheme = colorScheme,
        currentTextSizeDp = textSizeDp,
        terminalTypeface = typeface,
        onTextSizeChange = { textSizeDp = it },
        onProcessExit = onExit,
        onFailure = onError,
        sessionHandle = sessionHandle
    )
}

@Composable
private fun EmbeddedGradleBuild(
    project: Project,
    task: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    sessionHandle: TerminalSessionHandle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val typeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.firacode_medium) ?: Typeface.MONOSPACE
    }
    var textSizeDp by rememberSaveable { mutableIntStateOf(12) }

    GradleTaskTerminal(
        context = context,
        projectRoot = project.root,
        task = task,
        colorScheme = colorScheme,
        currentTextSizeDp = textSizeDp,
        terminalTypeface = typeface,
        onTextSizeChange = { textSizeDp = it },
        onTaskSuccess = onSuccess,
        onTaskError = onError,
        sessionHandle = sessionHandle,
        modifier = modifier
    )
}

private const val ResizeHandleHeightDp = 16f
private const val TabsHeightDp = 48f
private const val CollapseSnapThresholdDp = 124f
