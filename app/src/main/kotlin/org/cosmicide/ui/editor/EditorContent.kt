package org.cosmicide.ui.editor

import android.util.Log
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways
import org.cosmicide.editor.EditorPreviewProvider
import org.cosmicide.editor.EditorPreviewRenderRequest
import org.cosmicide.project.Project
import java.io.File

@Composable
internal fun TextEditorContent(
    editor: CodeEditor,
    project: Project,
    file: File,
    previewProvider: EditorPreviewProvider?
) {
    var charset by remember { mutableStateOf("UTF-8") }
    var position by remember { mutableStateOf("1:1") }
    var statsDialogShown by remember { mutableStateOf(false) }
    var showingPreview by rememberSaveable(file.absolutePath) { mutableStateOf(false) }
    var previewContent by remember(file.absolutePath) { mutableStateOf("") }
    val supportsPreview = previewProvider != null

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
        Box(modifier = Modifier.fillMaxSize()) {
            if (showingPreview && supportsPreview) {
                PreviewProviderContent(
                    provider = previewProvider,
                    project = project,
                    file = file,
                    content = previewContent,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                key(editor) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { editor }
                    )
                }
            }

            if (supportsPreview) {
                EditorPreviewSwitch(
                    showingPreview = showingPreview,
                    onShowCode = { showingPreview = false },
                    onShowPreview = {
                        previewContent = editor.text.toString()
                        showingPreview = true
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }
    }

    if (statsDialogShown) {
        Statistics(editor.text) {
            statsDialogShown = false
        }
    }
}

@Composable
private fun EditorPreviewSwitch(
    showingPreview: Boolean,
    onShowCode: () -> Unit,
    onShowPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(3.dp)
        ) {
            PreviewModeButton(
                selected = !showingPreview,
                onClick = onShowCode,
                imageVector = Icons.Rounded.Code,
                contentDescription = "Show code"
            )
            PreviewModeButton(
                selected = showingPreview,
                onClick = onShowPreview,
                imageVector = Icons.Rounded.Visibility,
                contentDescription = "Show preview"
            )
        }
    }
}

@Composable
private fun PreviewModeButton(
    selected: Boolean,
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
internal fun PreviewProviderContent(
    provider: EditorPreviewProvider,
    project: Project,
    file: File,
    content: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val contentColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val request = EditorPreviewRenderRequest(
        context = context,
        project = project,
        file = file,
        content = content,
        backgroundColor = backgroundColor,
        contentColor = contentColor
    )

    key(provider.id, file.absolutePath) {
        AndroidView(
            modifier = modifier.background(MaterialTheme.colorScheme.surface),
            factory = {
                runCatching { provider.createView(request) }
                    .getOrElse { error ->
                        TextView(it).apply {
                            setTextColor(contentColor)
                            setBackgroundColor(backgroundColor)
                            text = "Preview failed: ${error.message.orEmpty()}"
                            setPadding(24, 24, 24, 24)
                        }
                    }
            },
            update = { view ->
                runCatching { provider.updateView(view, request) }
                    .onFailure {
                        Log.w("EditorPreview", "Provider ${provider.id} failed to update", it)
                    }
            },
            onRelease = { view ->
                runCatching { provider.releaseView(view) }
                    .onFailure {
                        Log.w("EditorPreview", "Provider ${provider.id} failed to release", it)
                    }
            }
        )
    }
}
