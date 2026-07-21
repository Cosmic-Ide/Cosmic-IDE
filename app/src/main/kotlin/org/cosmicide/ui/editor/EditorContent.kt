package org.cosmicide.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways

@Composable
internal fun TextEditorContent(editor: CodeEditor) {
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

