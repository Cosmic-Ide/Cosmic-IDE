package org.cosmicide.ui.compile

import android.content.Context
import android.graphics.Typeface
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.TerminalView
import org.cosmicide.common.Prefs
import org.cosmicide.ui.terminal.BasicTerminalViewClient
import org.cosmicide.ui.terminal.ExtraKeysBar
import org.cosmicide.ui.terminal.MaxTerminalTextSizeDp
import org.cosmicide.ui.terminal.MinTerminalTextSizeDp
import org.cosmicide.ui.terminal.TerminalController
import org.cosmicide.ui.terminal.TerminalModifierLatch
import org.cosmicide.ui.terminal.applyTerminalColors
import org.cosmicide.util.jdksDir
import java.io.File

@Composable
internal fun CommandTerminal(
    modifier: Modifier = Modifier,
    context: Context,
    workingDirectory: File,
    commandLine: String,
    commandArguments: List<String>? = null,
    colorScheme: ColorScheme,
    currentTextSizeDp: Int,
    terminalTypeface: Typeface,
    onTextSizeChange: (Int) -> Unit,
    onProcessExit: (Int) -> Unit,
    onFailure: (String) -> Unit,
    environmentOverrides: Map<String, String> = emptyMap(),
    sessionHandle: TerminalSessionHandle? = null
) {
    val scope = rememberCoroutineScope()
    val modifierLatch = remember { TerminalModifierLatch() }
    var activeController by remember { mutableStateOf<TerminalController?>(null) }
    val currentOnProcessExit by rememberUpdatedState(onProcessExit)
    val currentOnFailure by rememberUpdatedState(onFailure)
    val currentOnTextSizeChange by rememberUpdatedState(onTextSizeChange)

    Column(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { viewContext ->
                val terminalView = TerminalView(viewContext, null).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(colorScheme.surfaceContainer.toArgb())
                isFocusable = true
                isFocusableInTouchMode = true
            }

            val controller = TerminalController(
                context = context.applicationContext,
                commandLine = commandLine,
                commandArguments = commandArguments,
                workingDir = workingDirectory,
                jdkDir = context.applicationContext.jdksDir().resolve(Prefs.currentJDK),
                environmentOverrides = environmentOverrides,
                terminalView = terminalView,
                modifierLatch = modifierLatch,
                scope = scope,
                onTitleChanged = {},
                onFailure = { error ->
                    currentOnFailure(error.message.orEmpty())
                },
                onProcessExit = { exitCode -> currentOnProcessExit(exitCode) }
            )
                activeController = controller
                sessionHandle?.bind(controller)
            val runtime = GradleTaskTerminalRuntime(
                controller = controller,
                textSizeDp = currentTextSizeDp,
                typeface = terminalTypeface
            )
            terminalView.tag = runtime

            terminalView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                view.post {
                    controller.startOrResize(runtime.textSizeDp, runtime.typeface)
                }
            }

            terminalView.setTerminalViewClient(
                BasicTerminalViewClient(
                    controller = controller,
                    showKeyboard = {
                        terminalView.requestFocus()
                        val imm = viewContext.getSystemService(InputMethodManager::class.java)
                        imm.showSoftInput(terminalView, 0)
                    },
                    onZoom = { increase ->
                        runtime.textSizeDp =
                            (runtime.textSizeDp + if (increase) 1 else -1)
                                .coerceIn(MinTerminalTextSizeDp, MaxTerminalTextSizeDp)
                        currentOnTextSizeChange(runtime.textSizeDp)
                    }
                )
            )

            terminalView.setOnClickListener {
                terminalView.requestFocus()
                val imm = viewContext.getSystemService(InputMethodManager::class.java)
                imm.showSoftInput(terminalView, 0)
            }

            terminalView.post {
                controller.startOrResize(currentTextSizeDp, terminalTypeface)
            }

                terminalView
            },
            update = { view ->
                view.setBackgroundColor(colorScheme.surfaceContainer.toArgb())
                applyTerminalColors(colorScheme)
                (view.tag as? GradleTaskTerminalRuntime)?.let { runtime ->
                    runtime.textSizeDp = currentTextSizeDp
                    runtime.typeface = terminalTypeface
                    runtime.controller.startOrResize(currentTextSizeDp, terminalTypeface)
                }
            },
            onRelease = { view ->
                (view.tag as? GradleTaskTerminalRuntime)?.controller?.let { controller ->
                    sessionHandle?.unbind(controller)
                    if (activeController === controller) activeController = null
                    controller.close()
                }
                view.tag = null
            }
        )

        ExtraKeysBar(
            controller = activeController,
            modifierLatch = modifierLatch,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}

internal class TerminalSessionHandle {
    private var controller: TerminalController? = null

    internal fun bind(controller: TerminalController) {
        this.controller = controller
    }

    internal fun unbind(controller: TerminalController) {
        if (this.controller === controller) this.controller = null
    }

    fun terminate() {
        controller?.terminate()
    }
}

private data class GradleTaskTerminalRuntime(
    val controller: TerminalController,
    var textSizeDp: Int,
    var typeface: Typeface
)
