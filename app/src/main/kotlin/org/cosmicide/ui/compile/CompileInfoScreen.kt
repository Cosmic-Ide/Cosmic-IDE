package org.cosmicide.ui.compile

import android.graphics.Typeface
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalOutput
import com.termux.view.TerminalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.R
import org.cosmicide.exec.PtyProcessExecutor
import org.cosmicide.ui.terminal.TerminalTranscriptRows
import org.cosmicide.ui.terminal.applyTerminalAppearance
import org.cosmicide.ui.terminal.applyTerminalColors
import org.cosmicide.ui.terminal.calculateGeometry
import org.cosmicide.util.ProjectHandler
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompileInfoScreen(
    onNavigateBack: () -> Unit,
    onCompileSuccess: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val project = remember {
        ProjectHandler.getProject() ?: throw IllegalStateException("No project set")
    }

    var buildError by remember { mutableStateOf<String?>(null) }

    val terminalTypeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.firacode_medium) ?: Typeface.MONOSPACE
    }
    var currentTextSizeDp by rememberSaveable { mutableIntStateOf(14) }

    DisposableEffect(colorScheme) {
        applyTerminalColors(colorScheme)
        onDispose {}
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Compiling ${project.name}",
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
                HorizontalDivider(thickness = 1.dp)
            }
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            buildError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            GradleBuildTerminal(
                context = context,
                projectRoot = project.root,
                colorScheme = colorScheme,
                currentTextSizeDp = currentTextSizeDp,
                terminalTypeface = terminalTypeface,
                onCompileSuccess = onCompileSuccess,
                onBuildError = { error -> buildError = error },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun GradleBuildTerminal(
    context: android.content.Context,
    projectRoot: File,
    colorScheme: androidx.compose.material3.ColorScheme,
    currentTextSizeDp: Int,
    terminalTypeface: Typeface,
    onCompileSuccess: () -> Unit,
    onBuildError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    AndroidView(
        modifier = modifier,
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

            val ptyProcess = PtyProcessExecutor.startGradleBuild(
                context = context,
                projectRoot = projectRoot,
                terminalRows = 25,
                terminalColumns = 80
            )

            val terminalOutput = object : TerminalOutput() {
                override fun write(data: ByteArray, offset: Int, count: Int) {
                    try {
                        ptyProcess.getOutputStream().write(data, offset, count)
                        ptyProcess.getOutputStream().flush()
                    } catch (_: Exception) {
                    }
                }

                override fun titleChanged(oldTitle: String?, newTitle: String?) {}
                override fun onCopyTextToClipboard(text: String?) {}
                override fun onPasteTextFromClipboard() {}
                override fun onBell() {}
                override fun onColorsChanged() {
                    terminalView.onScreenUpdated()
                }
            }

            val emulator = TerminalEmulator(
                terminalOutput, 80, 25, 1, 1,
                TerminalTranscriptRows, null
            )
            terminalView.mEmulator = emulator
            terminalView.invalidate()

            var readerJob: Job? = null

            terminalView.post {
                val font = ResourcesCompat.getFont(viewContext, R.font.firacode_medium)
                    ?: Typeface.MONOSPACE
                val textSizePx = viewContext.resources.displayMetrics.density * currentTextSizeDp
                terminalView.mRenderer = com.termux.view.TerminalRenderer(textSizePx.toInt(), font)
                terminalView.invalidate()

                val calcGeometry = terminalView.calculateGeometry(currentTextSizeDp, font)
                calcGeometry?.let { geom ->
                    emulator.resize(
                        geom.columns,
                        geom.rows,
                        geom.cellWidthPixels,
                        geom.cellHeightPixels
                    )
                    ptyProcess.setTerminalSize(geom.rows, geom.columns)
                    terminalView.invalidate()
                }

                readerJob = scope.launch(Dispatchers.IO) {
                    val inputStream = ptyProcess.getInputStream()
                    val buffer = ByteArray(8192)
                    while (isActive) {
                        try {
                            val read = inputStream.read(buffer)
                            if (read <= 0) break
                            withContext(Dispatchers.Main) {
                                emulator.append(buffer, read)
                                terminalView.onScreenUpdated()
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                    val exitCode = ptyProcess.waitFor()
                    withContext(Dispatchers.Main) {
                        if (exitCode == 0) onCompileSuccess()
                        else onBuildError("Build failed with exit code $exitCode")
                    }
                }
            }

            terminalView.setTerminalViewClient(object : com.termux.view.TerminalViewClient {
                override fun onScale(scale: Float) = scale
                override fun onSingleTapUp(e: android.view.MotionEvent) {
                    terminalView.requestFocus()
                    val imm = context.getSystemService(InputMethodManager::class.java)
                    imm.showSoftInput(terminalView, 0)
                }

                override fun onKeyDown(
                    keyCode: Int,
                    e: android.view.KeyEvent,
                    session: com.termux.terminal.TerminalSession?
                ): Boolean {
                    return try {
                        if (e.action == android.view.KeyEvent.ACTION_DOWN) {
                            val out = ptyProcess.getOutputStream()
                            when (keyCode) {
                                android.view.KeyEvent.KEYCODE_ENTER -> {
                                    out.write('\n'.code); out.flush(); }

                                android.view.KeyEvent.KEYCODE_DEL -> {
                                    out.write(127); out.flush(); }

                                else -> {
                                    val c = e.unicodeChar
                                    if (c > 0) {
                                        out.write(c.toString().toByteArray()); out.flush(); }
                                }
                            }
                        }
                        false
                    } catch (_: Exception) {
                        false
                    }
                }

                override fun onKeyUp(keyCode: Int, e: android.view.KeyEvent) = false
                override fun onCodePoint(
                    codePoint: Int,
                    ctrlDown: Boolean,
                    session: com.termux.terminal.TerminalSession?
                ): Boolean {
                    return try {
                        val out = ptyProcess.getOutputStream()
                        if (codePoint in 0..31 || codePoint == 127) out.write(
                            codePoint.toByte().toInt()
                        )
                        else out.write(codePoint.toString().toByteArray())
                        out.flush()
                        true
                    } catch (_: Exception) {
                        false
                    }
                }

                override fun shouldBackButtonBeMappedToEscape() = false
                override fun shouldEnforceCharBasedInput() = false
                override fun shouldUseCtrlSpaceWorkaround() = false
                override fun isTerminalViewSelected() = true
                override fun copyModeChanged(copyMode: Boolean) = Unit
                override fun onLongPress(event: android.view.MotionEvent) = false
                override fun readControlKey() = false
                override fun readAltKey() = false
                override fun readShiftKey() = false
                override fun readFnKey() = false
                override fun onEmulatorSet() = Unit
                override fun logError(tag: String, message: String) = Unit
                override fun logWarn(tag: String, message: String) = Unit
                override fun logInfo(tag: String, message: String) = Unit
                override fun logDebug(tag: String, message: String) = Unit
                override fun logVerbose(tag: String, message: String) = Unit
                override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) =
                    Unit

                override fun logStackTrace(tag: String, e: Exception) = Unit
            })

            terminalView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}
                override fun onViewDetachedFromWindow(v: View) {
                    readerJob?.cancel()
                    scope.launch(Dispatchers.IO) {
                        ptyProcess.terminate()
                        try {
                            ptyProcess.waitFor()
                        } catch (_: Exception) {
                        }
                        ptyProcess.close()
                    }
                }
            })

            terminalView
        },
        update = { view ->
            view.setBackgroundColor(colorScheme.surfaceContainer.toArgb())
            applyTerminalColors(colorScheme)
            val font =
                ResourcesCompat.getFont(view.context, R.font.firacode_medium) ?: Typeface.MONOSPACE
            view.calculateGeometry(currentTextSizeDp, font)?.let { geom ->
                view.mEmulator?.takeIf { it.mColumns != geom.columns || it.mRows != geom.rows }
                    ?.apply {
                        resize(geom.columns, geom.rows, geom.cellWidthPixels, geom.cellHeightPixels)
                        view.invalidate()
                    }
            }
        }
    )
}
