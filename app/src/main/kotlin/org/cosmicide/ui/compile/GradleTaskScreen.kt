package org.cosmicide.ui.compile

import android.graphics.Typeface
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
import com.termux.view.TerminalView
import org.cosmicide.R
import org.cosmicide.common.Prefs
import org.cosmicide.project.Project
import org.cosmicide.ui.terminal.BasicTerminalViewClient
import org.cosmicide.ui.terminal.MaxTerminalTextSizeDp
import org.cosmicide.ui.terminal.MinTerminalTextSizeDp
import org.cosmicide.ui.terminal.TerminalController
import org.cosmicide.ui.terminal.TerminalModifierLatch
import org.cosmicide.ui.terminal.applyTerminalColors
import org.cosmicide.util.jdksDir
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradleTaskScreen(
    project: Project,
    task: String,
    onNavigateBack: () -> Unit,
    onTaskSuccess: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var taskError by remember { mutableStateOf<String?>(null) }

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
                            text = "$task · ${project.name}",
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
            taskError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            GradleTaskTerminal(
                context = context,
                projectRoot = project.root,
                task = task,
                colorScheme = colorScheme,
                currentTextSizeDp = currentTextSizeDp,
                terminalTypeface = terminalTypeface,
                onTextSizeChange = { currentTextSizeDp = it },
                onTaskSuccess = onTaskSuccess,
                onTaskError = { error -> taskError = error },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
internal fun GradleTaskTerminal(
    context: android.content.Context,
    projectRoot: File,
    task: String,
    colorScheme: androidx.compose.material3.ColorScheme,
    currentTextSizeDp: Int,
    terminalTypeface: Typeface,
    onTextSizeChange: (Int) -> Unit,
    onTaskSuccess: () -> Unit,
    onTaskError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CommandTerminal(
        context = context,
        workingDirectory = projectRoot,
        commandLine = "./gradlew $task",
        colorScheme = colorScheme,
        currentTextSizeDp = currentTextSizeDp,
        terminalTypeface = terminalTypeface,
        onTextSizeChange = onTextSizeChange,
        onProcessExit = { exitCode ->
            if (exitCode == 0) onTaskSuccess()
            else onTaskError("Gradle task '$task' failed with exit code $exitCode")
        },
        onFailure = { error ->
            onTaskError("Gradle task '$task' failed: $error")
        },
        modifier = modifier
    )
}

@Composable
internal fun CommandTerminal(
    context: android.content.Context,
    workingDirectory: File,
    commandLine: String,
    commandArguments: List<String>? = null,
    colorScheme: androidx.compose.material3.ColorScheme,
    currentTextSizeDp: Int,
    terminalTypeface: Typeface,
    onTextSizeChange: (Int) -> Unit,
    onProcessExit: (Int) -> Unit,
    onFailure: (String) -> Unit,
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
                isFocusable = true
                isFocusableInTouchMode = true
            }

            val controller = TerminalController(
                context = context.applicationContext,
                commandLine = commandLine,
                commandArguments = commandArguments,
                workingDir = workingDirectory,
                jdkDir = context.applicationContext.jdksDir().resolve(Prefs.currentJDK),
                terminalView = terminalView,
                modifierLatch = TerminalModifierLatch(),
                scope = scope,
                onTitleChanged = {},
                onFailure = { error ->
                    onFailure(error.message.orEmpty())
                },
                onProcessExit = onProcessExit
            )
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
                        onTextSizeChange(runtime.textSizeDp)
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
            (view.tag as? GradleTaskTerminalRuntime)?.controller?.close()
            view.tag = null
        }
    )
}

private data class GradleTaskTerminalRuntime(
    val controller: TerminalController,
    var textSizeDp: Int,
    var typeface: Typeface
)
