package org.cosmicide.ui.terminal

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.termux.view.TerminalView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cosmicide.R
import org.cosmicide.common.Prefs
import org.cosmicide.util.jdksDir
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

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

    var currentTextSizeDp by rememberSaveable(terminalTextSizeDp) {
        mutableIntStateOf(terminalTextSizeDp.coerceIn(MinTerminalTextSizeDp, MaxTerminalTextSizeDp))
    }
    var nextSessionId by remember { mutableIntStateOf(2) }

    fun createSession(id: Int): TerminalUiSession {
        return createTerminalUiSession(
            id = id,
            viewContext = context,
            controllerContext = appContext,
            commandLine = initialCommand,
            workingDir = terminalWorkingDir,
            setup = setup,
            jdkDir = jdkDir,
            typeface = terminalTypeface,
            textSizeDp = { currentTextSizeDp },
            onTextSizeChanged = { currentTextSizeDp = it },
            scope = scope,
            colorScheme = colorScheme,
            onProcessExit = onProcessExit
        )
    }

    val sessions = remember {
        mutableStateListOf(createSession(id = 1))
    }
    var activeSessionId by remember { mutableIntStateOf(1) }
    val fallbackModifierLatch = remember { TerminalModifierLatch() }

    val activeSession = sessions.firstOrNull { it.id == activeSessionId }
        ?: sessions.firstOrNull()

    fun addSession() {
        val session = createSession(nextSessionId++)
        sessions.add(session)
        activeSessionId = session.id
    }

    fun closeSession(session: TerminalUiSession) {
        val removedIndex = sessions.indexOf(session)
        val wasActive = session.id == activeSessionId

        session.controller.close()
        sessions.remove(session)

        if (sessions.isEmpty()) {
            onNavigateBack()
            return
        }

        if (wasActive) {
            activeSessionId = sessions[
                removedIndex.coerceIn(0, sessions.lastIndex)
            ].id
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sessions.forEach { it.controller.close() }
        }
    }

    DisposableEffect(colorScheme) {
        applyTerminalColors(colorScheme)
        sessions.forEach { session ->
            session.terminalView.setBackgroundColor(colorScheme.surfaceContainer.toArgb())
            session.terminalView.onScreenUpdated()
        }
        onDispose {}
    }

    Scaffold(
        topBar = {
            Column {
                TerminalTopBar(
                    title = activeSession?.displayTitle ?: initialCommand,
                    sessionCount = sessions.size,
                    colorScheme = colorScheme,
                    onNavigateBack = onNavigateBack,
                    onAddSession = ::addSession,
                )

                TerminalSessionSwitcher(
                    sessions = sessions,
                    activeSessionId = activeSession?.id,
                    onSelect = { activeSessionId = it.id },
                    onClose = ::closeSession
                )
            }
        },
        bottomBar = {
            ExtraKeysBar(
                controller = activeSession?.controller,
                modifierLatch = activeSession?.modifierLatch ?: fallbackModifierLatch,
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(2.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { padding ->
        if (activeSession != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp, 8.dp, 8.dp, 2.dp),
                factory = { viewContext ->
                    FrameLayout(viewContext).also { host ->
                        host.attachSession(
                            session = activeSession,
                            colorScheme = colorScheme,
                            textSizeDp = currentTextSizeDp,
                            typeface = terminalTypeface
                        )
                    }
                },
                update = { host ->
                    host.attachSession(
                        session = activeSession,
                        colorScheme = colorScheme,
                        textSizeDp = currentTextSizeDp,
                        typeface = terminalTypeface
                    )
                },
                onRelease = { host ->
                    host.removeAllViews()
                    host.tag = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalTopBar(
    title: String,
    sessionCount: Int,
    colorScheme: ColorScheme,
    onNavigateBack: () -> Unit,
    onAddSession: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$sessionCount terminal ${if (sessionCount == 1) "session" else "sessions"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            IconButton(onClick = onAddSession) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New terminal session"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.surfaceContainer
        )
    )
}

@Composable
private fun TerminalSessionSwitcher(
    sessions: List<TerminalUiSession>,
    activeSessionId: Int?,
    onSelect: (TerminalUiSession) -> Unit,
    onClose: (TerminalUiSession) -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sessions.forEachIndexed { index, session ->
                val selected = session.id == activeSessionId

                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onSelect(session) },
                            modifier = Modifier
                                .height(36.dp)
                                .widthIn(min = 76.dp, max = 180.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        ) {
                            Text(
                                text = "${index + 1}  ${session.displayTitle}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        IconButton(
                            onClick = { onClose(session) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close session ${index + 1}",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExtraKeysBar(
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
                    repeatOnHold = key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP ||
                            key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN ||
                            key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
                            key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
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
    repeatOnHold: Boolean = false,
    onClick: () -> Unit
) {
    if (repeatOnHold) {
        RepeatableExtraKeyButton(label, selected, onClick)
        return
    }

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

@Composable
private fun RepeatableExtraKeyButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val currentOnClick by androidx.compose.runtime.rememberUpdatedState(onClick)

    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 44.dp)
            .semantics {
                role = Role.Button
                onClick {
                    currentOnClick()
                    true
                }
            }
            .pointerInput(Unit) {
                coroutineScope repeatScope@{
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var repeated = false
                        val repeatJob = this@repeatScope.launch {
                            delay(400.milliseconds)
                            repeated = true
                            while (true) {
                                currentOnClick()
                                delay(70.milliseconds)
                            }
                        }

                        val released = waitForUpOrCancellation() != null
                        repeatJob.cancel()
                        if (released && !repeated) currentOnClick()
                    }
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private class TerminalUiSession(
    val id: Int,
    val initialCommand: String
) {
    var title by mutableStateOf(initialCommand)
    var startupError by mutableStateOf<String?>(null)

    val modifierLatch = TerminalModifierLatch()
    lateinit var terminalView: TerminalView
    lateinit var controller: TerminalController

    val displayTitle: String
        get() = startupError ?: title.ifBlank { initialCommand }
}

private fun createTerminalUiSession(
    id: Int,
    viewContext: Context,
    controllerContext: Context,
    commandLine: String,
    workingDir: File,
    setup: Boolean,
    jdkDir: File,
    typeface: Typeface,
    textSizeDp: () -> Int,
    onTextSizeChanged: (Int) -> Unit,
    scope: CoroutineScope,
    colorScheme: ColorScheme,
    onProcessExit: (Int) -> Unit
): TerminalUiSession {
    val session = TerminalUiSession(id, commandLine)

    val terminalView = TerminalView(viewContext, null).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(colorScheme.surfaceContainer.toArgb())
        applyTerminalAppearance(textSizeDp(), typeface)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    val controller = TerminalController(
        context = controllerContext,
        commandLine = commandLine,
        workingDir = workingDir,
        setup = setup,
        jdkDir = jdkDir,
        terminalView = terminalView,
        modifierLatch = session.modifierLatch,
        scope = scope,
        onTitleChanged = { newTitle ->
            session.startupError = null
            session.title = newTitle.ifBlank { commandLine }
        },
        onFailure = { error ->
            session.startupError = "Terminal fault: ${error.message.orEmpty()}"
        },
        onProcessExit = onProcessExit
    )

    session.terminalView = terminalView
    session.controller = controller

    terminalView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        view.post {
            controller.startOrResize(textSizeDp(), typeface)
        }
    }

    terminalView.setTerminalViewClient(
        BasicTerminalViewClient(
            controller = controller,
            showKeyboard = terminalView::showKeyboard,
            onZoom = { increase ->
                val next = (textSizeDp() + if (increase) 1 else -1)
                    .coerceIn(MinTerminalTextSizeDp, MaxTerminalTextSizeDp)
                onTextSizeChanged(next)
            }
        )
    )

    terminalView.setOnClickListener {
        terminalView.showKeyboard()
    }

    return session
}

private fun FrameLayout.attachSession(
    session: TerminalUiSession,
    colorScheme: ColorScheme,
    textSizeDp: Int,
    typeface: Typeface
) {
    val view = session.terminalView
    val changedSession = tag != session.id || getChildAt(0) !== view

    setBackgroundColor(colorScheme.surfaceContainer.toArgb())
    view.setBackgroundColor(colorScheme.surfaceContainer.toArgb())

    if (changedSession) {
        (view.parent as? ViewGroup)?.removeView(view)
        removeAllViews()
        addView(view)
        tag = session.id
    }

    session.controller.startOrResize(textSizeDp, typeface)

    if (changedSession) {
        view.post {
            view.showKeyboard()
        }
    }
}

private fun TerminalView.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, 0)
}

