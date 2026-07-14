package org.cosmicide.ui.terminal

import android.content.Context
import android.graphics.Typeface
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.termux.view.TerminalView
import org.cosmicide.R
import org.cosmicide.common.Prefs
import org.cosmicide.util.jdksDir
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    initialCommand: String = "bash -i",
    workingDir: File? = null,
    setup: Boolean = false,
    terminalTextSizeDp: Int = 14,
    terminalFontResId: Int = R.font.firacode_medium,
    onProcessExit: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    val jdkDir = remember { appContext.jdksDir().resolve(Prefs.currentJDK) }
    val terminalWorkingDir = remember(workingDir) { workingDir ?: appContext.filesDir }
    val terminalTypeface = remember(context, terminalFontResId) {
        ResourcesCompat.getFont(context, terminalFontResId) ?: Typeface.MONOSPACE
    }

    var title by remember { mutableStateOf(initialCommand) }
    var startupError by remember { mutableStateOf<String?>(null) }
    var terminalController by remember { mutableStateOf<TerminalController?>(null) }

    val modifierLatch = remember { TerminalModifierLatch() }
    var currentTextSizeDp by rememberSaveable(terminalTextSizeDp) {
        mutableIntStateOf(terminalTextSizeDp.coerceIn(MinTerminalTextSizeDp, MaxTerminalTextSizeDp))
    }

    DisposableEffect(colorScheme) {
        applyTerminalColors(colorScheme)
        onDispose {}
    }

    Scaffold(
        topBar = {
            TerminalTopBar(
                title = startupError ?: title,
                colorScheme = colorScheme,
                onNavigateBack = onNavigateBack,
                onClose = { terminalController?.terminate() }
            )
        },
        bottomBar = {
            ExtraKeysBar(
                controller = terminalController,
                modifierLatch = modifierLatch,
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(2.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp, 8.dp, 8.dp, 2.dp),
            factory = { viewContext ->
                val terminalView = TerminalView(viewContext, null).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(colorScheme.surfaceContainer.toArgb())
                    applyTerminalAppearance(currentTextSizeDp, terminalTypeface)
                    isFocusable = true
                    isFocusableInTouchMode = true
                }


                val controller = TerminalController(
                    context = appContext,
                    commandLine = initialCommand,
                    workingDir = terminalWorkingDir,
                    setup = setup,
                    jdkDir = jdkDir,
                    terminalView = terminalView,
                    modifierLatch = modifierLatch,
                    scope = scope,
                    onTitleChanged = { title = it.ifBlank { initialCommand } },
                    onFailure = { startupError = "Terminal fault: ${it.message.orEmpty()}" },
                    onProcessExit = onProcessExit
                )

                terminalView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    view.post {
                        controller.startOrResize(currentTextSizeDp, terminalTypeface)
                    }
                }

                terminalView.setTerminalViewClient(
                    BasicTerminalViewClient(
                        controller = controller,
                        showKeyboard = terminalView::showKeyboard,
                        onZoom = { increase ->
                            currentTextSizeDp = (currentTextSizeDp + if (increase) 1 else -1)
                                .coerceIn(MinTerminalTextSizeDp, MaxTerminalTextSizeDp)
                        }
                    )
                )

                terminalView.setOnClickListener {
                    terminalView.showKeyboard()
                }

                val host = FrameLayout(viewContext).apply {
                    setBackgroundColor(colorScheme.surfaceContainer.toArgb())
                    addView(terminalView)
                }

                terminalController = controller

                terminalView.post {
                    controller.startOrResize(currentTextSizeDp, terminalTypeface)
                    terminalView.showKeyboard()
                }

                host
            },
            update = { host ->
                host.setBackgroundColor(colorScheme.surfaceContainer.toArgb())
                terminalController?.startOrResize(currentTextSizeDp, terminalTypeface)
            },
            onRelease = {
                terminalController?.close()
                terminalController = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalTopBar(
    title: String,
    colorScheme: ColorScheme,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close terminal session"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.surfaceContainer
            )
        )
    }
}

@Composable
private fun ExtraKeysBar(
    controller: TerminalController?,
    modifierLatch: TerminalModifierLatch,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    fun clearModifiers() {
        modifierLatch.ctrl = false
        modifierLatch.alt = false
    }

    fun send(extraKey: ExtraKey) {
        val ctrlDown = modifierLatch.ctrl
        val altDown = modifierLatch.alt

        when {
            extraKey.codePoint != null -> {
                controller?.writeTerminalCodePoint(
                    prependEscape = altDown,
                    codePoint = extraKey.codePoint.toTerminalCodePoint(ctrlDown)
                )
            }

            extraKey.keyCode != null -> {
                controller?.handleKeyCode(
                    keyCode = extraKey.keyCode,
                    ctrlPressed = ctrlDown,
                    altPressed = altDown
                )
            }

            extraKey.output != null -> {
                val output = when {
                    ctrlDown && extraKey.output.length == 1 ->
                        extraKey.output.single().controlChar()

                    altDown ->
                        Escape + extraKey.output

                    else ->
                        extraKey.output
                }

                controller?.write(output)
            }
        }

        clearModifiers()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExtraKeyButton(
                label = "ESC",
                selected = false,
                onClick = { controller?.write(Escape) }
            )

            ExtraKeyButton(
                label = "CTRL",
                selected = modifierLatch.ctrl,
                onClick = {
                    modifierLatch.ctrl = !modifierLatch.ctrl
                    if (modifierLatch.ctrl) modifierLatch.alt = false
                }
            )

            ExtraKeyButton(
                label = "ALT",
                selected = modifierLatch.alt,
                onClick = {
                    modifierLatch.alt = !modifierLatch.alt
                    if (modifierLatch.alt) modifierLatch.ctrl = false
                }
            )

            ExtraKeyButton(
                label = "^C",
                selected = false,
                onClick = {
                    controller?.write(byteArrayOf(EndOfText.toByte()), 0, 1)
                    controller?.interrupt()
                    clearModifiers()
                }
            )

            TerminalExtraKeys.forEach { key ->
                ExtraKeyButton(
                    label = key.label,
                    selected = false,
                    onClick = { send(key) }
                )
            }
        }
    }
}

@Composable
private fun ExtraKeyButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 44.dp),
        colors = if (selected) {
            ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        },
        shapes = ButtonDefaults.shapes()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun TerminalView.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, 0)
}
