package org.cosmicide.editor.lsp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ComposeViewContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.workspace.workSpaceApplyEdit
import io.github.rosemoe.sora.lsp.events.workspace.workSpaceExecuteCommand
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.widget.component.DiagnosticTooltipLayout
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.cosmicide.R
import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.CodeActionTriggerKind
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.TextDocumentEdit
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import android.graphics.Color as AndroidColor

/**
 * A Compose diagnostic card hosted by Sora's Android tooltip window.
 *
 * Code actions are requested only when a diagnostic is shown, keeping diagnostic publishing cheap
 * and ensuring the proposed edits match the current document state.
 */
class LspDiagnosticTooltipLayout(
    private val lspEditor: LspEditor,
    private val colorScheme: ColorScheme
) : DiagnosticTooltipLayout {

    private lateinit var window: EditorDiagnosticTooltipWindow
    private lateinit var composeView: ComposeView

    private var currentDiagnostic: DiagnosticDetail? = null
    private var currentRegion: DiagnosticRegion? = null
    private var requestGeneration = 0
    private var pointerOverPopup = false
    private var editorTextSizePx by mutableFloatStateOf(14f)
    private var uiState by mutableStateOf(DiagnosticTooltipState())

    override fun attach(window: EditorDiagnosticTooltipWindow) {
        this.window = window
        window.parentView.setBackgroundColor(AndroidColor.TRANSPARENT)
    }

    override fun createView(inflater: LayoutInflater): View {
        editorTextSizePx = window.editor.textSizePx
        composeView = ComposeView(inflater.context).apply {
            // Selection handles create a second Compose popup. Sora's PopupWindow is a separate
            // view tree, so explicitly expose the activity owners that popup needs.
            setViewTreeLifecycleOwner(window.editor.findViewTreeLifecycleOwner())
            setViewTreeViewModelStoreOwner(window.editor.findViewTreeViewModelStoreOwner())
            setViewTreeSavedStateRegistryOwner(
                window.editor.findViewTreeSavedStateRegistryOwner()
            )
            setBackgroundColor(AndroidColor.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setOnGenericMotionListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_HOVER_ENTER -> pointerOverPopup = true
                    MotionEvent.ACTION_HOVER_EXIT -> pointerOverPopup = false
                }
                false
            }
            setContent {
                MaterialTheme(colorScheme = colorScheme) {
                    val textSize = with(LocalDensity.current) {
                        editorTextSizePx.toSp()
                    }
                    DiagnosticTooltipCard(
                        state = uiState,
                        editorTextSize = textSize,
                        outlineColor = colorScheme.outlineVariant,
                        onCopy = ::copyDiagnostic,
                        onAction = { item ->
                            executeAction(item.action)
                            window.dismiss()
                        },
                        onContentChanged = ::resizeWindow
                    )
                }
            }

            // Sora measures tooltip content before the PopupWindow is attached. Reusing the
            // editor's Compose context makes that first off-tree measurement safe.
            createComposition(ComposeViewContext(window.editor))
        }
        return composeView
    }

    override fun applyColorScheme(colorScheme: EditorColorScheme) {
    }

    override fun renderDiagnostic(diagnostic: DiagnosticDetail?) {
        renderDiagnostic(diagnostic, currentRegion)
    }

    override fun renderDiagnostic(
        diagnostic: DiagnosticDetail?,
        region: DiagnosticRegion?
    ) {
        currentDiagnostic = diagnostic
        currentRegion = region
        requestGeneration++

        if (diagnostic == null) {
            uiState = DiagnosticTooltipState()
            return
        }

        uiState = DiagnosticTooltipState(
            message = diagnostic.detailedMessage
                ?.takeIf(CharSequence::isNotEmpty)
                ?: diagnostic.briefMessage,
            severity = region.toTooltipSeverity()
        )

        val lspDiagnostic = diagnostic.extraData as? Diagnostic
        if (lspDiagnostic == null || !lspEditor.isConnected) {
            return
        }

        loadQuickFixes(lspDiagnostic, requestGeneration)
    }

    override fun onTextSizeChanged(oldSizePx: Float, newSizePx: Float) {
        if (newSizePx <= 0f) return
        editorTextSizePx = newSizePx
    }

    override fun measureContent(maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
        )
        return composeView.measuredWidth.coerceAtMost(maxWidth) to
                composeView.measuredHeight.coerceAtMost(maxHeight)
    }

    override fun isPointerOverPopup(): Boolean = pointerOverPopup

    override fun isMenuShowing(): Boolean = false

    override fun onWindowDismissed() {
        pointerOverPopup = false
        requestGeneration++
    }

    private fun loadQuickFixes(diagnostic: Diagnostic, generation: Int) {
        uiState = uiState.copy(fixes = QuickFixState.Loading)

        val params = CodeActionParams(
            lspEditor.uri.createTextDocumentIdentifier(),
            diagnostic.range,
            CodeActionContext(listOf(diagnostic)).apply {
                triggerKind = CodeActionTriggerKind.Invoked
            }
        )

        lspEditor.coroutineScope.launch(Dispatchers.IO) {
            val actions = runCatching {
                withTimeout(Timeout[Timeouts.CODEACTION].toLong().milliseconds) {
                    lspEditor.requestManager.codeAction(params)?.await().orEmpty()
                }
            }.getOrElse {
                LspLogStore.warning("LSP", "Unable to load quick fixes", it)
                emptyList()
            }

            withContext(Dispatchers.Main) {
                if (generation != requestGeneration || currentDiagnostic?.extraData !== diagnostic) {
                    return@withContext
                }
                renderQuickFixes(actions)
            }
        }
    }

    private fun renderQuickFixes(actions: List<Either<Command, CodeAction>>) {
        val items = actions
            .filterNot { it.isRight && it.right?.disabled != null }
            .map { action ->
                val codeAction = if (action.isRight) action.right else null
                QuickFixItem(
                    title = if (action.isLeft) action.left?.title else codeAction?.title,
                    isPreferred = codeAction?.isPreferred == true,
                    action = action
                )
            }
            .sortedByDescending(QuickFixItem::isPreferred)

        uiState = uiState.copy(
            fixes = if (items.isEmpty()) {
                QuickFixState.Empty
            } else {
                QuickFixState.Available(items)
            }
        )
    }

    private fun executeAction(action: Either<Command, CodeAction>) {
        if (action.isLeft) {
            executeCommand(action.left)
            return
        }

        val codeAction = action.right ?: return
        codeAction.edit?.let { edit ->
            val params = ApplyWorkspaceEditParams().apply {
                label = codeAction.title
                this.edit = edit.normalizedForSora()
            }
            lspEditor.eventManager.emit(EventType.workSpaceApplyEdit, params)
        }
        executeCommand(codeAction.command)
    }

    private fun executeCommand(command: Command?) {
        val commandId = command?.command ?: return
        val args = command.arguments ?: emptyList()
        lspEditor.coroutineScope.launch {
            lspEditor.eventManager.emitAsync(EventType.workSpaceExecuteCommand) {
                put("command", commandId)
                put("args", args)
            }
        }
    }

    /**
     * Sora 0.24.6 looks up `WorkspaceEdit.changes` using the raw file URI string. Converting that
     * representation to document changes routes it through Sora's correctly normalized URI path.
     */
    private fun WorkspaceEdit.normalizedForSora(): WorkspaceEdit {
        val plainChanges = changes ?: return this
        if (documentChanges != null) return this

        return WorkspaceEdit(
            plainChanges.map { (uri, edits) ->
                val documentEdit = TextDocumentEdit(
                    VersionedTextDocumentIdentifier(uri, null),
                    edits.map { edit ->
                        Either.forLeft(edit)
                    }
                )
                Either.forLeft(documentEdit)
            }
        ).also {
            it.changeAnnotations = changeAnnotations
        }
    }

    private fun copyDiagnostic() {
        val message = currentDiagnostic?.detailedMessage
            ?.takeIf(CharSequence::isNotEmpty)
            ?: currentDiagnostic?.briefMessage
            ?: return
        val clipboard = window.editor.context
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostic", message))
    }

    private fun resizeWindow() {
        if (!::composeView.isInitialized) return
        composeView.requestLayout()
        composeView.post {
            if (!::composeView.isInitialized) return@post
            val editor = window.editor
            val maxWidth = (editor.width * 0.92f).toInt().coerceAtLeast(1)
            val availableHeight = (editor.height * 0.55f).toInt()
            val maxHeight = minOf(dp(300), availableHeight.coerceAtLeast(dp(160)))
            val (width, height) = measureContent(maxWidth, maxHeight)
            window.setSize(width, height)
        }
    }

    private fun dp(value: Int): Int = (window.editor.dpUnit * value).toInt()
}

private data class DiagnosticTooltipState(
    val message: CharSequence = "",
    val severity: TooltipSeverity = TooltipSeverity.Diagnostic,
    val fixes: QuickFixState = QuickFixState.Hidden
)

private enum class TooltipSeverity {
    Error,
    Warning,
    Info,
    Diagnostic
}

private sealed interface QuickFixState {
    data object Hidden : QuickFixState
    data object Loading : QuickFixState
    data object Empty : QuickFixState
    data class Available(val items: List<QuickFixItem>) : QuickFixState
}

private data class QuickFixItem(
    val title: String?,
    val isPreferred: Boolean,
    val action: Either<Command, CodeAction>
)

private data class SeverityStyle(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val container: Color
)

private fun DiagnosticRegion?.toTooltipSeverity(): TooltipSeverity {
    return when (this?.severity) {
        DiagnosticRegion.SEVERITY_ERROR -> TooltipSeverity.Error
        DiagnosticRegion.SEVERITY_WARNING -> TooltipSeverity.Warning
        DiagnosticRegion.SEVERITY_TYPO -> TooltipSeverity.Info
        else -> TooltipSeverity.Diagnostic
    }
}

@Composable
private fun DiagnosticTooltipCard(
    state: DiagnosticTooltipState,
    editorTextSize: TextUnit,
    outlineColor: Color,
    onCopy: () -> Unit,
    onAction: (QuickFixItem) -> Unit,
    onContentChanged: () -> Unit
) {
    val scrollState = rememberScrollState()
    var copied by remember(state.message) { mutableStateOf(false) }
    val severity = state.severity.style()

    LaunchedEffect(state, editorTextSize, outlineColor) {
        onContentChanged()
    }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1.6.seconds)
            copied = false
        }
    }

    Surface(
        modifier = Modifier.widthIn(min = 240.dp, max = 480.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.72f)),
        tonalElevation = 5.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 240.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(severity.container.copy(alpha = 0.42f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = severity.container,
                    contentColor = severity.accent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = severity.icon,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = severity.label,
                    modifier = Modifier.weight(1f),
                    color = severity.accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                TextButton(
                    onClick = {
                        onCopy()
                        copied = true
                    }
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (copied) {
                            "Copied"
                        } else {
                            stringResource(R.string.lsp_diagnostic_copy)
                        }
                    )
                }
            }

            Text(
                text = state.message.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.SansSerif,
                fontSize = editorTextSize * 0.9f,
                lineHeight = editorTextSize * 1.28f
            )

            when (val fixes = state.fixes) {
                QuickFixState.Hidden -> Unit
                QuickFixState.Loading -> QuickFixLoading()
                QuickFixState.Empty -> QuickFixEmpty()
                is QuickFixState.Available -> QuickFixList(
                    items = fixes.items,
                    fontSize = 14.sp,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun QuickFixHeader(count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 11.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Lightbulb,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.lsp_diagnostic_quick_fixes),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (count != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickFixLoading() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    QuickFixHeader()
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(15.dp),
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = "Looking for code actions…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun QuickFixEmpty() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    QuickFixHeader()
    Text(
        text = "No fixes available for this issue",
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun QuickFixList(
    items: List<QuickFixItem>,
    fontSize: TextUnit,
    onAction: (QuickFixItem) -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    QuickFixHeader(items.size)
    Column(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { _, item ->
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(item) }
                        .padding(start = 8.dp, end = 2.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (item.isPreferred) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.title?.takeIf(String::isNotBlank) ?: "Unnamed action",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = fontSize,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.isPreferred) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "Preferred",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Apply code action",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TooltipSeverity.style(): SeverityStyle {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        TooltipSeverity.Error -> SeverityStyle(
            label = "ERROR",
            icon = Icons.Rounded.ErrorOutline,
            accent = colors.error,
            container = colors.errorContainer
        )

        TooltipSeverity.Warning -> SeverityStyle(
            label = "WARNING",
            icon = Icons.Rounded.WarningAmber,
            accent = colors.tertiary,
            container = colors.tertiaryContainer
        )

        TooltipSeverity.Info -> SeverityStyle(
            label = "INFO",
            icon = Icons.Rounded.Info,
            accent = colors.primary,
            container = colors.primaryContainer
        )

        TooltipSeverity.Diagnostic -> SeverityStyle(
            label = "DIAGNOSTIC",
            icon = Icons.Rounded.Info,
            accent = colors.secondary,
            container = colors.secondaryContainer
        )
    }
}
