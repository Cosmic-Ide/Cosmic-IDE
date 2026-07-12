package org.cosmicide.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.BuildConfig
import org.cosmicide.common.Prefs
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.ui.output.MemoryInstrumentationPane
import org.cosmicide.ui.settings.components.PreferenceItem
import org.cosmicide.ui.settings.components.SwitchPreference
import org.cosmicide.util.jdksDir
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var analyticsEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                "analytics_preference",
                true
            )
        )
    }
    var showExecutionSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("About") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            })
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceItem(
                title = "About",
                summary = "A free and open-source IDE for Android. It is licensed under the GNU General Public License v3.0."
            )

            PreferenceItem(
                title = "Donate",
                summary = "Donate to the developers. This will help us to keep the project alive. Thank you for your support!",
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW, "https://pranavpurwar.github.io/donate.html".toUri()
                        )
                    )
                })

            PreferenceItem(
                title = "App version", summary = BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) {
                    " (${BuildConfig.GIT_COMMIT})"
                } else {
                    ""
                }, onClick = {
                    // Logic for developer mode could go here
                })

            PreferenceItem(
                title = "Execute Linux command",
                summary = "Run arbitrary commands in a full-screen execution sheet",
                onClick = { showExecutionSheet = true })

            PreferenceItem(
                title = "Source code", onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW, "https://github.com/Cosmic-IDE/Cosmic-IDE".toUri()
                        )
                    )
                })

            PreferenceItem(
                title = "Manage storage permission", onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    ("package:" + context.packageName).toUri()
                                )
                            )
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    } else {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                ("package:" + context.packageName).toUri()
                            )
                        )
                    }
                })

            SwitchPreference(
                title = "Analytics",
                summary = "Help us improve the app by sending anonymous usage data",
                checked = analyticsEnabled,
                onCheckedChange = {
                    analyticsEnabled = it
                    prefs.edit { putBoolean("analytics_preference", it) }
                })
        }
    }

    if (showExecutionSheet) {
        TerminalExecutionSheet(
            onDismiss = { showExecutionSheet = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalExecutionSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)

    val jdkDir = remember { context.jdksDir().resolve(Prefs.currentJDK) }
    val workingDir = remember { context.filesDir }

    var commandInput by remember { mutableStateOf("") }
    var outputLog by remember { mutableStateOf("Environment ready for command...\n") }
    var isRunning by remember { mutableStateOf(false) }
    var currentProcess by remember { mutableStateOf<Process?>(null) }
    var runningCommandIsJava by remember { mutableStateOf(false) }
    var isInstrumentationExpanded by remember { mutableStateOf(true) }

    val outputScrollState = rememberScrollState()
    val latestProcess by rememberUpdatedState(currentProcess)

    fun stopRunningProcess() {
        try {
            currentProcess?.destroyForcibly()
        } catch (_: Exception) {
        }

        currentProcess = null
        isRunning = false
        runningCommandIsJava = false
    }

    fun closeSheet() {
        stopRunningProcess()
        onDismiss()
    }

    fun runCommand() {
        if (commandInput.isBlank() || isRunning) return

        isRunning = true
        currentProcess = null
        runningCommandIsJava = false
        outputLog = "Executing: ${commandInput.trim()}\n\n"

        scope.launch(Dispatchers.IO) {
            try {
                val tempDir = context.cacheDir
                val pathEntries = LinuxProcessRunner.toolchainPathEntries(context, jdkDir)
                val commandParts = LinuxProcessRunner.parseCommandLine(commandInput.trim())

                if (commandParts.isEmpty()) {
                    throw IllegalArgumentException("No command provided")
                }

                val binary = LinuxProcessRunner.resolveExecutable(
                    commandName = commandParts.first(),
                    workingDir = workingDir,
                    pathEntries = pathEntries
                )

                val isJavaCommand = isJavaExecutable(
                    commandName = commandParts.first(), binary = binary
                )

                withContext(Dispatchers.Main) {
                    runningCommandIsJava = isJavaCommand
                }

                val processArguments = commandParts.drop(1).toMutableList()
                if (isJavaCommand && processArguments.none { it == "-XX:+UsePerfData" || it == "-XX:-UsePerfData" }) {
                    processArguments.add(0, "-XX:+UsePerfData")
                }

                val runnerConfig = LinuxProcessRunner.Configuration(
                    binary = binary,
                    arguments = processArguments,
                    workingDir = workingDir,
                    environmentOverrides = LinuxProcessRunner.toolchainEnvironment(
                        jdkDir
                    ) + mapOf(
                        "TMPDIR" to tempDir.absolutePath,
                        "TMP" to tempDir.absolutePath,
                        "TEMP" to tempDir.absolutePath
                    ),
                    pathEntries = pathEntries
                )

                LinuxProcessRunner.execute(
                    context = context,
                    config = runnerConfig,
                    onOutputReceived = { outputChunk ->
                        scope.launch(Dispatchers.Main) {
                            outputLog += outputChunk
                            outputScrollState.animateScrollTo(outputScrollState.maxValue)
                        }
                    },
                    onProcessStarted = { process ->
                        scope.launch(Dispatchers.Main) {
                            currentProcess = process
                        }
                    })
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    outputLog += "\nExecution Fault: ${e.message.orEmpty()}\n"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    currentProcess = null
                    isRunning = false
                    runningCommandIsJava = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                latestProcess?.destroyForcibly()
            } catch (_: Exception) {
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { closeSheet() },
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(), topBar = {
                Column {
                    TopAppBar(title = {
                        Text(
                            text = "Execution Panel",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }, navigationIcon = {
                        IconButton(onClick = { closeSheet() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close execution panel"
                            )
                        }
                    }, actions = {
                        if (isRunning) {
                            IconButton(
                                onClick = {
                                    outputLog += "\n--- Stopped ---\n"
                                    stopRunningProcess()
                                }) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop process"
                                )
                            }
                        }
                    })

                    HorizontalDivider(thickness = 1.dp)
                }
            }) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    label = { Text("Command") },
                    placeholder = { Text("e.g., java -version") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    enabled = !isRunning,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { runCommand() })
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Button(
                        onClick = { runCommand() },
                        enabled = commandInput.isNotBlank() && !isRunning,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = { outputLog = "" }, enabled = !isRunning && outputLog.isNotEmpty()
                    ) {
                        Text("Clear")
                    }
                }

                if (isRunning) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(4.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                }

                Card(
                    shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ), modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = outputLog,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace, lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(outputScrollState)
                            )
                        }
                    }
                }

                if (runningCommandIsJava && currentProcess != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    MemoryInstrumentationPane(
                        context = context,
                        process = currentProcess!!,
                        jdkDir = jdkDir,
                        workingDir = workingDir,
                        expanded = isInstrumentationExpanded,
                        onToggleExpanded = {
                            isInstrumentationExpanded = !isInstrumentationExpanded
                        })
                }
            }
        }
    }
}

private fun isJavaExecutable(
    commandName: String, binary: File
): Boolean {
    val requestedName = File(commandName).name
    val binaryName = binary.name

    return requestedName == "java" || requestedName == "java.exe" || binaryName == "java" || binaryName == "java.exe" || binaryName.endsWith(
        "/bin/java"
    ) || binaryName.endsWith("/bin/java.exe")
}
