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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import org.cosmicide.R
import org.cosmicide.project.Project
import org.cosmicide.ui.compile.GradleTaskTerminal

internal const val SyncToolWindowTabId = "sync"
internal const val CollapsedEditorToolWindowHeightDp = 64f
internal const val DefaultEditorToolWindowHeightDp = 280f

internal data class EditorBuildSession(
    val id: Int,
    val task: String,
    val runId: Int = 0,
    val status: String = "Running"
) {
    val tabId: String
        get() = "build:$id"
}

@Composable
internal fun EditorToolWindowLayout(
    project: Project,
    toolingOutput: String,
    selectedTabId: String,
    heightDp: Float,
    buildSessions: List<EditorBuildSession>,
    onSelectTab: (String) -> Unit,
    onHeightChange: (Float) -> Unit,
    onCloseBuild: (Int) -> Unit,
    onRerunBuild: (Int) -> Unit,
    onBuildStatusChange: (Int, String) -> Unit,
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
            editorContent()

            EditorToolWindow(
                project = project,
                toolingOutput = toolingOutput,
                selectedTabId = selectedTabId,
                heightDp = resolvedHeight,
                buildSessions = buildSessions,
                onSelectTab = onSelectTab,
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
                onCloseBuild = onCloseBuild,
                onRerunBuild = onRerunBuild,
                onBuildStatusChange = onBuildStatusChange,
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
    toolingOutput: String,
    selectedTabId: String,
    heightDp: Float,
    buildSessions: List<EditorBuildSession>,
    onSelectTab: (String) -> Unit,
    onResize: (Float) -> Unit,
    onResizeFinished: () -> Unit,
    onCloseBuild: (Int) -> Unit,
    onRerunBuild: (Int) -> Unit,
    onBuildStatusChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                                    onClick = { onCloseBuild(session.id) },
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
                SyncTab(
                    output = toolingOutput,
                    modifier = Modifier.visibleToolWindowTab(
                        selectedTabId == SyncToolWindowTabId
                    )
                )

                buildSessions.forEach { session ->
                    key(session.id) {
                        BuildTab(
                            project = project,
                            session = session,
                            onRerun = { onRerunBuild(session.id) },
                            onStatusChange = { status ->
                                onBuildStatusChange(session.id, status)
                            },
                            modifier = Modifier.visibleToolWindowTab(
                                selectedTabId == session.tabId
                            )
                        )
                    }
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
private fun SyncTab(
    output: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.maxValue) {
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
                text = output.ifEmpty { "No sync output yet." },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BuildTab(
    project: Project,
    session: EditorBuildSession,
    onRerun: () -> Unit,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            IconButton(onClick = onRerun, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Rerun ${session.task}")
            }
        }
        HorizontalDivider()

        key(session.runId) {
            EmbeddedGradleBuild(
                project = project,
                task = session.task,
                onSuccess = { onStatusChange("Finished") },
                onError = onStatusChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun EmbeddedGradleBuild(
    project: Project,
    task: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
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
        modifier = modifier
    )
}

private const val ResizeHandleHeightDp = 16f
private const val TabsHeightDp = 48f
private const val CollapseSnapThresholdDp = 124f
